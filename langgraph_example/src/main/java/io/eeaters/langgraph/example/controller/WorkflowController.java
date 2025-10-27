package io.eeaters.langgraph.example.controller;

import io.eeaters.langgraph.example.model.WorkflowExecution;
import io.eeaters.langgraph.example.service.TaskService;
import io.eeaters.langgraph.example.service.WorkflowExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    @Autowired
    private TaskService taskService;

    @GetMapping("/status")
    public Map<String, Object> getWorkflowStatus() {
        Map<String, Object> status = new HashMap<>();

        List<WorkflowExecution> allExecutions = workflowExecutionService.getAllExecutions();
        List<WorkflowExecution> waitingExecutions = workflowExecutionService.getExecutionsWaitingForReply();
        List<WorkflowExecution> readyExecutions = workflowExecutionService.getExecutionsReadyForRetry();

        status.put("totalExecutions", allExecutions.size());
        status.put("waitingForReply", waitingExecutions.size());
        status.put("readyForRetry", readyExecutions.size());
        status.put("totalTasks", taskService.getTaskCount());

        Map<String, Integer> statusCount = new HashMap<>();
        allExecutions.forEach(exec -> {
            String statusKey = exec.getStatus().toString();
            statusCount.put(statusKey, statusCount.getOrDefault(statusKey, 0) + 1);
        });

        status.put("executionStatusCounts", statusCount);
        status.put("executions", allExecutions);

        logger.info("Workflow status requested: {} executions, {} waiting for reply",
                   allExecutions.size(), waitingExecutions.size());

        return status;
    }

    @GetMapping("/executions")
    public List<WorkflowExecution> getAllExecutions() {
        return workflowExecutionService.getAllExecutions();
    }

    @GetMapping("/waiting")
    public List<WorkflowExecution> getWaitingExecutions() {
        return workflowExecutionService.getExecutionsWaitingForReply();
    }
}