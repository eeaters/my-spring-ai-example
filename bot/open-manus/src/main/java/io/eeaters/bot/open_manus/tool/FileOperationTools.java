package io.eeaters.bot.open_manus.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Component
public class FileOperationTools {

    private static final Logger logger = LoggerFactory.getLogger(FileOperationTools.class);

    @Tool(description = "读取文件内容")
    public String readFile(ReadRequest request) {
        try {
            Path path = Paths.get(request.path());

            if (!isPathSafe(path)) {
                return "错误：文件路径不安全，只允许在当前工作目录内操作文件";
            }

            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            return "文件内容:\n" + Files.readString(path);
        } catch (Exception e) {
            logger.error("Read file failed", e);
            return "读取文件失败: " + e.getMessage();
        }
    }

    @Tool(description = "写入文件内容")
    public String writeFile(WriteRequest request) {
        try {
            Path path = Paths.get(request.path());

            if (!isPathSafe(path)) {
                return "错误：文件路径不安全，只允许在当前工作目录内操作文件";
            }

            // 确保父目录存在
            Files.createDirectories(path.getParent());

            Files.writeString(path, request.content() != null ? request.content() : "",
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING);
            return "文件写入成功: " + path;
        } catch (Exception e) {
            logger.error("Write file failed", e);
            return "写入文件失败: " + e.getMessage();
        }
    }


    @Tool(description = "删除文件")
    public String deleteFile(DeleteRequest request) {
        try {
            Path path = Paths.get(request.path());

            if (!isPathSafe(path)) {
                return "错误：文件路径不安全，只允许在当前工作目录内操作文件";
            }

            if (!Files.exists(path)) {
                return "错误：文件不存在 - " + path;
            }

            Files.delete(path);
            return "文件删除成功: " + path;
        } catch (Exception e) {
            logger.error("Delete file failed", e);
            return "删除文件失败: " + e.getMessage();
        }
    }

    private boolean isPathSafe(Path path) {
        try {
            Path currentDir = Paths.get("").toAbsolutePath().normalize();
            Path normalizedPath = path.toAbsolutePath().normalize();
            return normalizedPath.startsWith(currentDir);
        } catch (Exception e) {
            return false;
        }
    }

    public record ReadRequest(
            @JsonPropertyDescription("要读取的文件路径") String path) {
    }

    public record WriteRequest(
            @JsonPropertyDescription("要写入的文件路径") String path,
            @JsonPropertyDescription("要写入的文件内容") String content) {
    }

    public record CreateRequest(
            @JsonPropertyDescription("要创建的文件路径") String path,
            @JsonPropertyDescription("创建时写入的文件内容") String content) {
    }

    public record DeleteRequest(
            @JsonPropertyDescription("要删除的文件路径") String path) {
    }
}
