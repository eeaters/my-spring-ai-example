package io.eeaters.bot.open_manus.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CodeEditorTools {

    private static final Logger logger = LoggerFactory.getLogger(CodeEditorTools.class);
    private static final int MAX_RESPONSE_LENGTH = 16000;
    private static final String TRUNCATED_MESSAGE = """
            <response clipped><NOTE>To save on context only part of this file has been shown to you.
            You should retry this tool after you have searched inside the file with `grep -n`
            in order to find the line numbers of what you are looking for.</NOTE>
            """;

    private final Map<String, Stack<String>> fileHistory = new HashMap<>();

    @Tool(description = "查看文件或目录内容。如果是文件，显示带行号的内容；如果是目录，显示文件和子目录列表（最多2层深）")
    public String viewFile(ViewRequest request) {
        try {
            Path path = Paths.get(request.path()).toAbsolutePath().normalize();

            if (!isPathSafe(path)) {
                return "错误：路径不安全，只能在当前工作目录内操作";
            }

            if (!Files.exists(path)) {
                return "错误：文件或目录不存在 - " + path;
            }

            if (Files.isDirectory(path)) {
                return listDirectory(path, request.maxDepth());
            } else {
                return readFileWithLineNumbers(path, Optional.of(request.viewRange()));
            }

        } catch (Exception e) {
            logger.error("View file failed", e);
            return "查看文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "创建新文件。如果文件已存在会报错")
    public String createFile(CreateRequest request) {
        try {
            Path path = Paths.get(request.path()).toAbsolutePath().normalize();

            if (!isPathSafe(path)) {
                return "错误：路径不安全，只能在当前工作目录内操作";
            }

            if (Files.exists(path)) {
                return "错误：文件已存在 - " + path;
            }

            // 确保父目录存在
            Files.createDirectories(path.getParent());

            // 创建文件并写入内容
            Files.writeString(path, request.fileText() != null ? request.fileText() : "");

            // 初始化历史记录
            fileHistory.computeIfAbsent(path.toString(), k -> new Stack<>()).push("");

            return "文件创建成功: " + path;

        } catch (Exception e) {
            logger.error("Create file failed", e);
            return "创建文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "字符串替换。精确匹配old_str并替换为new_str。old_str必须与文件中的内容完全一致")
    public String strReplace(StrReplaceRequest request) {
        try {
            Path path = Paths.get(request.path()).toAbsolutePath().normalize();

            if (!isPathSafe(path)) {
                return "错误：路径不安全，只能在当前工作目录内操作";
            }

            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            String content = Files.readString(path);

            if (!content.contains(request.oldStr())) {
                return "错误：未找到要替换的字符串。请确保old_str与文件中的内容完全一致（包括空格和换行）";
            }

            // 检查是否有多个匹配
            int count = countOccurrences(content, request.oldStr());
            if (count > 1) {
                return "错误：找到多个匹配的字符串。请提供更多上下文使old_str唯一";
            }

            // 保存当前内容到历史记录
            fileHistory.computeIfAbsent(path.toString(), k -> new Stack<>()).push(content);

            // 执行替换
            String newContent = content.replace(request.oldStr(), request.newStr());
            Files.writeString(path, newContent);

            return "替换成功：已替换1处内容";

        } catch (Exception e) {
            logger.error("String replace failed", e);
            return "字符串替换失败: " + e.getMessage();
        }
    }

    @Tool(description = "在指定行后插入内容。insert_line是要插入的行号，new_str是要插入的内容")
    public String insertText(InsertRequest request) {
        try {
            Path path = Paths.get(request.path()).toAbsolutePath().normalize();

            if (!isPathSafe(path)) {
                return "错误：路径不安全，只能在当前工作目录内操作";
            }

            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            List<String> lines = Files.readAllLines(path);

            if (request.insertLine() < 0 || request.insertLine() > lines.size()) {
                return "错误：行号超出范围。文件共有 " + lines.size() + " 行";
            }

            // 保存当前内容到历史记录
            String content = String.join("\n", lines);
            fileHistory.computeIfAbsent(path.toString(), k -> new Stack<>()).push(content);

            // 插入内容
            lines.add(request.insertLine(), request.newStr());
            String newContent = String.join("\n", lines);
            Files.writeString(path, newContent);

            return "插入成功：在第 " + request.insertLine() + " 行后插入了内容";

        } catch (Exception e) {
            logger.error("Insert text failed", e);
            return "插入文本失败: " + e.getMessage();
        }
    }

    @Tool(description = "撤销最后一次编辑操作")
    public String undoEdit(UndoRequest request) {
        try {
            Path path = Paths.get(request.path()).toAbsolutePath().normalize();

            if (!isPathSafe(path)) {
                return "错误：路径不安全，只能在当前工作目录内操作";
            }

            Stack<String> history = fileHistory.get(path.toString());
            if (history == null || history.isEmpty()) {
                return "错误：没有可撤销的操作";
            }

            String previousContent = history.pop();
            Files.writeString(path, previousContent);

            return "撤销成功：已恢复到上一个版本";

        } catch (Exception e) {
            logger.error("Undo edit failed", e);
            return "撤销操作失败: " + e.getMessage();
        }
    }

    private boolean isPathSafe(Path path) {
        try {
            Path currentDir = Paths.get("").toAbsolutePath().normalize();
            return path.startsWith(currentDir);
        } catch (Exception e) {
            return false;
        }
    }

    private String listDirectory(Path dir, int maxDepth) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("目录内容: ").append(dir).append("\n\n");

        int depth = maxDepth > 0 ? maxDepth : 2;

        Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), depth, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                int level = dir.getNameCount() - dir.getRoot().getNameCount();
                if (level > 0) {
                    sb.append("  ".repeat(level - 1)).append("📁 ").append(dir.getFileName()).append("\n");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                int level = file.getNameCount() - file.getRoot().getNameCount();
                sb.append("  ".repeat(level)).append("📄 ").append(file.getFileName()).append("\n");
                return FileVisitResult.CONTINUE;
            }
        });

        return sb.toString();
    }

    private String readFileWithLineNumbers(Path file, Optional<List<Integer>> viewRange) throws IOException {
        List<String> lines = Files.readAllLines(file);
        StringBuilder sb = new StringBuilder();

        int startLine = 1;
        int endLine = lines.size();

        if (viewRange.isPresent() && viewRange.get().size() >= 1) {
            startLine = Math.max(1, viewRange.get().get(0));
            if (viewRange.get().size() >= 2 && viewRange.get().get(1) != -1) {
                endLine = Math.min(lines.size(), viewRange.get().get(1));
            }
        }

        startLine = Math.max(1, startLine);
        endLine = Math.min(lines.size(), endLine);

        sb.append("文件内容: ").append(file).append("\n\n");

        for (int i = startLine - 1; i < endLine; i++) {
            sb.append(String.format("%4d: %s\n", i + 1, lines.get(i)));
        }

        String result = sb.toString();
        if (result.length() > MAX_RESPONSE_LENGTH) {
            return result.substring(0, MAX_RESPONSE_LENGTH) + TRUNCATED_MESSAGE;
        }

        return result;
    }

    private int countOccurrences(String content, String target) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }

    // Request records
    public record ViewRequest(
            @JsonPropertyDescription("要查看的文件或目录路径") String path,
            @JsonPropertyDescription("目录显示的最大深度（默认2）") Integer maxDepth,
            @JsonPropertyDescription("文件显示的行号范围 [start, end]，end为-1表示到文件末尾") List<Integer> viewRange
    ) {}

    public record CreateRequest(
            @JsonPropertyDescription("要创建的文件路径") String path,
            @JsonPropertyDescription("文件内容") String fileText
    ) {}

    public record StrReplaceRequest(
            @JsonPropertyDescription("文件路径") String path,
            @JsonPropertyDescription("要替换的原始字符串（必须完全匹配）") String oldStr,
            @JsonPropertyDescription("替换后的新字符串") String newStr
    ) {}

    public record InsertRequest(
            @JsonPropertyDescription("文件路径") String path,
            @JsonPropertyDescription("插入位置的行号（0-based）") Integer insertLine,
            @JsonPropertyDescription("要插入的内容") String newStr
    ) {}

    public record UndoRequest(
            @JsonPropertyDescription("文件路径") String path
    ) {}
}