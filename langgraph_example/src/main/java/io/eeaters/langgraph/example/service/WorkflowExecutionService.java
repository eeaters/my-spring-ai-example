package io.eeaters.langgraph.example.service;

import io.eeaters.langgraph.example.model.WorkflowExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowExecutionService {

	private static final Logger logger = LoggerFactory.getLogger(WorkflowExecutionService.class);

	// 简单的内存存储，实际项目中应该使用数据库
	private final Map<String, WorkflowExecution> executionStore = new ConcurrentHashMap<>();

	private final Map<String, String> taskIdToExecutionId = new ConcurrentHashMap<>();

	public WorkflowExecution saveExecution(WorkflowExecution execution) {
		if (execution.getId() == null) {
			execution.setId(UUID.randomUUID().toString());
		}

		execution.setUpdatedAt(LocalDateTime.now());
		executionStore.put(execution.getId(), execution);
		taskIdToExecutionId.put(execution.getTaskId(), execution.getId());

		logger.info("Saved workflow execution: {} for task: {}", execution.getId(), execution.getTaskId());
		return execution;
	}

	public WorkflowExecution getExecution(String executionId) {
		return executionStore.get(executionId);
	}

	public WorkflowExecution getExecutionByTaskId(String taskId) {
		String executionId = taskIdToExecutionId.get(taskId);
		return executionId != null ? executionStore.get(executionId) : null;
	}

	public List<WorkflowExecution> getExecutionsByStatus(WorkflowExecution.WorkflowStatus status) {
		return executionStore.values()
			.stream()
			.filter(execution -> execution.getStatus() == status)
			.sorted(Comparator.comparing(WorkflowExecution::getNextRetryAt))
			.toList();
	}

	public List<WorkflowExecution> getExecutionsReadyForRetry() {
		return executionStore.values()
			.stream()
			.filter(WorkflowExecution::isReadyForRetry)
			.sorted(Comparator.comparing(WorkflowExecution::getNextRetryAt))
			.toList();
	}

	public List<WorkflowExecution> getExecutionsWaitingForReply() {
		return executionStore.values()
			.stream()
			.filter(WorkflowExecution::isWaitingForReply)
			.sorted(Comparator.comparing(WorkflowExecution::getNextRetryAt))
			.toList();
	}

	public WorkflowExecution updateExecutionStatus(String executionId, WorkflowExecution.WorkflowStatus newStatus) {
		WorkflowExecution execution = executionStore.get(executionId);
		if (execution != null) {
			execution.updateStatus(newStatus);
			logger.info("Updated execution {} status to: {}", executionId, newStatus);
		}
		return execution;
	}

	public WorkflowExecution updateExecutionStep(String executionId, String newStep) {
		WorkflowExecution execution = executionStore.get(executionId);
		if (execution != null) {
			execution.updateStep(newStep);
			logger.info("Updated execution {} step to: {}", executionId, newStep);
		}
		return execution;
	}

	public WorkflowExecution incrementRetryCount(String executionId) {
		WorkflowExecution execution = executionStore.get(executionId);
		if (execution != null) {
			execution.incrementRetryCount();
			logger.info("Incremented retry count for execution {} to: {}", executionId, execution.getRetryCount());
		}
		return execution;
	}

	public WorkflowExecution deleteExecution(String executionId) {
		WorkflowExecution execution = executionStore.remove(executionId);
		if (execution != null) {
			taskIdToExecutionId.remove(execution.getTaskId());
			logger.info("Deleted workflow execution: {}", executionId);
		}
		return execution;
	}

	public List<WorkflowExecution> getAllExecutions() {
		return new ArrayList<>(executionStore.values());
	}

	public void cleanupOldExecutions(int daysToKeep) {
		LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
		List<String> toDelete = executionStore.values()
			.stream()
			.filter(execution -> execution.getUpdatedAt().isBefore(cutoffDate))
			.map(WorkflowExecution::getId)
			.toList();

		toDelete.forEach(this::deleteExecution);

		if (!toDelete.isEmpty()) {
			logger.info("Cleaned up {} old workflow executions", toDelete.size());
		}
	}

	public WorkflowExecution createWaitingExecution(String taskId, String threadId, String lastEmailId) {
		WorkflowExecution execution = WorkflowExecution.builder()
			.taskId(taskId)
			.threadId(threadId)
			.currentStep("WAIT_FOR_REPLY")
			.status(WorkflowExecution.WorkflowStatus.WAITING_FOR_REPLY)
			.type(WorkflowExecution.WorkflowType.EMAIL_COORDINATION)
			.lastEmailId(lastEmailId)
			.build();

		return saveExecution(execution);
	}

}