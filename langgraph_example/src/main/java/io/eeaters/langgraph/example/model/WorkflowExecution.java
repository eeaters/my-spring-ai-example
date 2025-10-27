package io.eeaters.langgraph.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecution {

	private String id;

	private String taskId;

	private String threadId;

	private String currentStep;

	private WorkflowStatus status;

	private WorkflowType type;

	private int retryCount;

	private LocalDateTime lastExecutedAt;

	private LocalDateTime nextRetryAt;

	private String lastEmailId;

	private String executionData;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

	@Default
	private LocalDateTime completedAt = null;

	public enum WorkflowStatus {

		PENDING, RUNNING, WAITING_FOR_REPLY, COMPLETED, FAILED, TERMINATED

	}

	public enum WorkflowType {

		EMAIL_COORDINATION

	}

	public WorkflowExecution(String taskId, String threadId, String currentStep, WorkflowStatus status,
			WorkflowType type) {
		this.id = java.util.UUID.randomUUID().toString();
		this.taskId = taskId;
		this.threadId = threadId;
		this.currentStep = currentStep;
		this.status = status;
		this.type = type;
		this.retryCount = 0;
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.lastExecutedAt = LocalDateTime.now();
		this.nextRetryAt = calculateNextRetryAt();
	}

	public void incrementRetryCount() {
		this.retryCount++;
		this.updatedAt = LocalDateTime.now();
		this.lastExecutedAt = LocalDateTime.now();
		this.nextRetryAt = calculateNextRetryAt();
	}

	public void updateStatus(WorkflowStatus newStatus) {
		this.status = newStatus;
		this.updatedAt = LocalDateTime.now();
		if (newStatus == WorkflowStatus.COMPLETED || newStatus == WorkflowStatus.FAILED
				|| newStatus == WorkflowStatus.TERMINATED) {
			this.completedAt = LocalDateTime.now();
		}
	}

	public void updateStep(String newStep) {
		this.currentStep = newStep;
		this.updatedAt = LocalDateTime.now();
		this.lastExecutedAt = LocalDateTime.now();
	}

	private LocalDateTime calculateNextRetryAt() {
		// 指数退避策略：1分钟, 2分钟, 4分钟, 8分钟, 最大30分钟
		long delayMinutes = Math.min((long) Math.pow(2, this.retryCount), 30);
		return LocalDateTime.now().plusMinutes(delayMinutes);
	}

	public boolean isReadyForRetry() {
		return LocalDateTime.now().isAfter(this.nextRetryAt)
				&& (this.status == WorkflowStatus.WAITING_FOR_REPLY || this.status == WorkflowStatus.PENDING);
	}

	public boolean isWaitingForReply() {
		return this.status == WorkflowStatus.WAITING_FOR_REPLY && "WAIT_FOR_REPLY".equals(this.currentStep);
	}

}