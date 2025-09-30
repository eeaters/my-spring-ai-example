package io.eeaters.bot.open_manus.tool;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Component
public class SystemTools {

    private static final Logger logger = LoggerFactory.getLogger(SystemTools.class);
    private static final long COMMAND_TIMEOUT_SECONDS = 120;
    private static final Pattern DANGEROUS_COMMANDS = Pattern.compile(
            "(rm\\s+-rf|dd\\s+if=|mkfs\\.|format\\s|fdisk\\s|shutdown\\s|reboot\\s|passwd\\s|su\\s|sudo\\s+su|chmod\\s+777|chown\\s+root)",
            Pattern.CASE_INSENSITIVE
    );

    private Process currentProcess = null;

    @Tool(description = "执行bash命令。支持各种系统操作，包括文件管理、进程管理、网络操作等。长时运行命令应使用后台执行。")
    public String executeBash(BashRequest request) {
        try {
            String command = request.command();

            if (command == null || command.trim().isEmpty()) {
                return "错误：命令不能为空";
            }

            // 安全检查
            if (isDangerousCommand(command)) {
                return "错误：检测到危险命令，拒绝执行。这个命令可能对系统造成损害。";
            }

            // 处理特殊命令
            if ("ctrl+c".equals(command.trim())) {
                return interruptCurrentProcess();
            }

            // 检查是否需要后台运行
            boolean backgroundRun = command.contains("&") || isLongRunningCommand(command);

            if (backgroundRun) {
                return executeInBackground(command);
            } else {
                return executeCommand(command);
            }

        } catch (Exception e) {
            logger.error("Bash execution failed", e);
            return "命令执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "执行Docker命令。支持容器管理、镜像操作、网络配置等Docker操作")
    public String executeDocker(DockerRequest request) {
        try {
            String command = request.command();

            if (command == null || command.trim().isEmpty()) {
                return "错误：Docker命令不能为空";
            }

            // 构建完整的Docker命令
            String fullCommand = "docker " + command;

            // 检查Docker是否可用
            if (!isDockerAvailable()) {
                return "错误：Docker不可用。请确保Docker已安装并正在运行。";
            }

            return executeCommand(fullCommand);

        } catch (Exception e) {
            logger.error("Docker execution failed", e);
            return "Docker命令执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "管理系统进程。可以查看进程列表、终止进程等")
    public String manageProcess(ProcessRequest request) {
        try {
            String action = request.action();

            switch (action.toLowerCase()) {
                case "list":
                    return executeCommand("ps aux");
                case "kill":
                    if (request.pid() == null) {
                        return "错误：终止进程需要提供进程ID (pid)";
                    }
                    return executeCommand("kill -TERM " + request.pid());
                case "force_kill":
                    if (request.pid() == null) {
                        return "错误：强制终止进程需要提供进程ID (pid)";
                    }
                    return executeCommand("kill -KILL " + request.pid());
                case "port":
                    if (request.port() == null) {
                        return "错误：查看端口占用需要提供端口号";
                    }
                    return executeCommand("lsof -i :" + request.port());
                default:
                    return "错误：不支持的操作。支持的操作：list, kill, force_kill, port";
            }

        } catch (Exception e) {
            logger.error("Process management failed", e);
            return "进程管理失败: " + e.getMessage();
        }
    }

    @Tool(description = "网络诊断工具。包括ping、curl、网络连接测试等")
    public String networkDiagnostic(NetworkRequest request) {
        try {
            String action = request.action();
            String target = request.target();

            if (target == null || target.trim().isEmpty()) {
                return "错误：网络诊断需要提供目标地址";
            }

            String command;
            switch (action.toLowerCase()) {
                case "ping":
                    command = "ping -c 4 " + target;
                    break;
                case "curl":
                    command = "curl -I " + target;
                    break;
                case "wget":
                    command = "wget --spider " + target;
                    break;
                case "nslookup":
                    command = "nslookup " + target;
                    break;
                case "telnet":
                    if (request.port() == null) {
                        return "错误：telnet需要提供端口号";
                    }
                    command = "timeout 5 telnet " + target + " " + request.port();
                    break;
                default:
                    return "错误：不支持的网络操作。支持的操作：ping, curl, wget, nslookup, telnet";
            }

            return executeCommand(command);

        } catch (Exception e) {
            logger.error("Network diagnostic failed", e);
            return "网络诊断失败: " + e.getMessage();
        }
    }

    private String executeCommand(String command) {
        try {
            logger.info("Executing command: {}", command);

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            currentProcess = process;

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待命令完成
            boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return "命令执行超时（超过" + COMMAND_TIMEOUT_SECONDS + "秒），已强制终止进程。\n部分输出：\n" + output;
            }

            int exitCode = process.exitValue();
            String result = output.toString().trim();

            if (exitCode != 0) {
                return "命令执行失败，退出码: " + exitCode + "\n输出：\n" +
                       (result.isEmpty() ? "无输出" : result);
            }

            return result.isEmpty() ? "命令执行完成，无输出" : result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "命令执行被中断";
        } catch (Exception e) {
            logger.error("Command execution failed", e);
            return "命令执行异常: " + e.getMessage();
        } finally {
            currentProcess = null;
        }
    }

    private String executeInBackground(String command) {
        try {
            // 确保命令以 & 结尾
            if (!command.endsWith("&")) {
                command += " &";
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(
                    new File("background_" + System.currentTimeMillis() + ".log")));

            Process process = pb.start();

            // 不等待后台进程完成
            return "后台命令已启动，进程ID: " + process.pid() + "\n命令: " + command;

        } catch (Exception e) {
            logger.error("Background execution failed", e);
            return "后台命令执行失败: " + e.getMessage();
        }
    }

    private String interruptCurrentProcess() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            try {
                if (!currentProcess.waitFor(5, TimeUnit.SECONDS)) {
                    currentProcess.destroyForcibly();
                }
                return "已发送中断信号给当前进程";
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "中断进程时被取消";
            }
        }
        return "没有正在运行的进程";
    }

    private boolean isDangerousCommand(String command) {
        return DANGEROUS_COMMANDS.matcher(command).find();
    }

    private boolean isLongRunningCommand(String command) {
        String lowerCommand = command.toLowerCase();
        return lowerCommand.contains("sleep") ||
               lowerCommand.contains("watch") ||
               lowerCommand.contains("ping") ||
               lowerCommand.contains("tail -f") ||
               lowerCommand.contains("server") ||
               lowerCommand.contains("daemon");
    }

    private boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "--version").start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Request records
    public record BashRequest(
            @JsonPropertyDescription("要执行的bash命令") String command
    ) {}

    public record DockerRequest(
            @JsonPropertyDescription("Docker命令参数（不包括'docker'前缀）") String command
    ) {}

    public record ProcessRequest(
            @JsonPropertyDescription("操作类型：list, kill, force_kill, port") String action,
            @JsonPropertyDescription("进程ID（用于kill操作）") Integer pid,
            @JsonPropertyDescription("端口号（用于port操作）") Integer port
    ) {}

    public record NetworkRequest(
            @JsonPropertyDescription("网络操作类型：ping, curl, wget, nslookup, telnet") String action,
            @JsonPropertyDescription("目标地址或域名") String target,
            @JsonPropertyDescription("端口号（telnet需要）") Integer port
    ) {}
}