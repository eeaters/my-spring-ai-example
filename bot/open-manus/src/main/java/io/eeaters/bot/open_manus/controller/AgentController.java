package io.eeaters.bot.open_manus.controller;

import io.eeaters.bot.open_manus.agent.ManusAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

    @Autowired
    private ManusAgent manusAgent;

    @GetMapping("/demo")
    public ResponseEntity<String> demoChat() {
        return ResponseEntity.ok("OpenManus API is running!");
    }

    @PostMapping("/run")
    public CompletableFuture<ResponseEntity<AgentResponse>> runTask(@RequestBody TaskRequest request) {
        logger.info("Received task: {}", request.getTask());

        return manusAgent.run(request.getTask())
                .thenApply(response -> {
                    AgentResponse agentResponse = new AgentResponse(response, true);
                    return ResponseEntity.ok(agentResponse);
                })
                .exceptionally(throwable -> {
                    logger.error("Task execution failed", throwable);
                    AgentResponse errorResponse = new AgentResponse("任务执行失败: " + throwable.getMessage(), false);
                    return ResponseEntity.status(500).body(errorResponse);
                });
    }


    // 请求和响应 DTO
    public static class TaskRequest {
        private String task;

        public TaskRequest() {}

        public TaskRequest(String task) {
            this.task = task;
        }

        public String getTask() {
            return task;
        }

        public void setTask(String task) {
            this.task = task;
        }
    }

    public static class AgentResponse {
        private String response;
        private boolean success;

        public AgentResponse(String response, boolean success) {
            this.response = response;
            this.success = success;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }
}
