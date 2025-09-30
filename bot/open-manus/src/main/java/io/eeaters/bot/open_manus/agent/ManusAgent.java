package io.eeaters.bot.open_manus.agent;

import io.eeaters.bot.open_manus.tool.CodeEditorTools;
import io.eeaters.bot.open_manus.tool.FileOperationTools;
import io.eeaters.bot.open_manus.tool.SystemTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ManusAgent {

    private static final Logger logger = LoggerFactory.getLogger(ManusAgent.class);

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Value("${openmanus.workspace:.}")
    private String workspace;

    @Value("${openmanus.max-steps:20}")
    private int maxSteps;


    @Autowired
    FileOperationTools fileOperationTools;

    @Autowired
    CodeEditorTools codeEditorTools;

    @Autowired
    SystemTools systemTools;


    private String buildSystemPrompt() {
        return """
            你是一个名为 Manus 的通用 AI 助手，能够使用多种工具来解决各种任务。

            工作空间: %s

            可用工具:

            文件操作工具:
            1. readFile - 读取文件内容
            2. writeFile - 写入文件内容
            3. createFile - 创建新文件
            4. deleteFile - 删除文件

            代码编辑工具:
            5. viewFile - 查看文件或目录内容（支持行号显示和行范围）
            6. createFileWithContent - 创建新文件并写入内容
            7. strReplace - 精确的字符串替换（需要完全匹配）
            8. insertText - 在指定行后插入内容
            9. undoEdit - 撤销最后一次编辑操作

            系统工具:
            10. pythonExecute - 执行 Python 代码
            11. executeBash - 执行bash命令（支持文件操作、进程管理等）
            12. executeDocker - 执行Docker命令（容器管理、镜像操作等）
            13. manageProcess - 管理系统进程（查看、终止进程等）
            14. networkDiagnostic - 网络诊断工具（ping、curl、nslookup等）

            指导原则:
            - 仔细分析用户的请求，选择合适的工具来完成任务
            - 如果任务复杂，可以分解为多个步骤
            - 使用代码编辑工具进行精确的文件修改，支持撤销操作
            - 执行系统命令前考虑安全性，避免危险操作
            - 长时间运行的命令应使用后台执行模式
            - 对于文件修改，优先使用strReplace而不是重写整个文件
            - 在不确定文件内容时，先用viewFile查看

            限制:
            - 最大执行步数: %d
            - 工作空间限制在指定目录内
            - 危险的系统命令会被拒绝执行
            """.formatted(workspace, maxSteps);
    }

    public CompletableFuture<String> run(String userPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Executing task: {}", userPrompt);

                String response = chatClientBuilder
                        .defaultSystem(buildSystemPrompt())
                        .defaultTools(
                                fileOperationTools,
                                codeEditorTools,
                                systemTools
                        ).build()
                        .prompt()
                        .user(userPrompt)
                        .call()
                        .content();

                logger.info("Task completed successfully");
                return response;

            } catch (Exception e) {
                logger.error("Error executing task", e);
                return "执行任务时发生错误: " + e.getMessage();
            }
        });
    }

}