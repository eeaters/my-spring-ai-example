package io.eeaters.langgraph.example.scheduler;

import io.eeaters.langgraph.example.model.EmailResponse;
import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.model.WorkflowExecution;
import io.eeaters.langgraph.example.service.EmailService;
import io.eeaters.langgraph.example.service.TaskService;
import io.eeaters.langgraph.example.service.WorkflowExecutionService;
import io.eeaters.langgraph.example.workflow.TaskWorkflow;
import io.eeaters.langgraph.example.workflow.WorkflowState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class EmailPollingScheduler {

	private static final Logger logger = LoggerFactory.getLogger(EmailPollingScheduler.class);

	@Autowired
	private WorkflowExecutionService workflowExecutionService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private TaskWorkflow taskWorkflow;

	/**
	 * 每30秒检查一次等待邮件回复的任务
	 */
	@Scheduled(fixedRate = 30000) // 30秒
	public void checkWaitingForReplyTasks() {
		logger.debug("Checking tasks waiting for email replies...");

		List<WorkflowExecution> waitingExecutions = workflowExecutionService.getExecutionsWaitingForReply();

		for (WorkflowExecution execution : waitingExecutions) {
			try {
				processWaitingExecution(execution);
			}
			catch (Exception e) {
				logger.error("Error processing waiting execution: {}", execution.getId(), e);
				workflowExecutionService.incrementRetryCount(execution.getId());
			}
		}

		if (!waitingExecutions.isEmpty()) {
			logger.info("Processed {} tasks waiting for email replies", waitingExecutions.size());
		}
	}

	/**
	 * 每5分钟清理过期的执行记录
	 */
	@Scheduled(fixedRate = 300000) // 5分钟
	public void cleanupOldExecutions() {
		workflowExecutionService.cleanupOldExecutions(7); // 保留7天
	}

	private void processWaitingExecution(WorkflowExecution execution) {
		logger.info("Processing waiting execution: {} for task: {}", execution.getId(), execution.getTaskId());

		// 获取任务信息
		Task task = taskService.getTask(execution.getTaskId());
		if (task == null) {
			logger.warn("Task not found for execution: {}", execution.getId());
			workflowExecutionService.updateExecutionStatus(execution.getId(), WorkflowExecution.WorkflowStatus.FAILED);
			return;
		}

		// 检查新邮件
		List<EmailResponse> responses = emailService.checkNewEmails(execution.getLastEmailId());

		if (responses.isEmpty()) {
			logger.info("No new emails for task: {}, execution: {}", task.getId(), execution.getId());
			workflowExecutionService.incrementRetryCount(execution.getId());
			return;
		}

		// 处理收到的邮件
		EmailResponse latestResponse = responses.get(responses.size() - 1);
		logger.info("Received email response for task: {}, subject: {}, isPositive: {}", task.getId(),
				latestResponse.getSubject(), latestResponse.isPositive());

		// 更新任务状态
		updateTaskBasedOnResponse(task, latestResponse);
		taskService.saveTask(task);

		// 恢复工作流执行
		resumeWorkflow(execution, task, latestResponse);
	}

	private void updateTaskBasedOnResponse(Task task, EmailResponse response) {
		if (response.isPositive()) {
			if (response.getProposedTime() != null) {
				task.setScheduledTime(response.getProposedTime());
				task.setFinalConfirmedTime(response.getProposedTime()
					.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
			}

			// 根据当前状态决定下一步
			if (task.getStatus() == TaskStatus.AUTO_REPLY_SENT) {
				task.setStatus(TaskStatus.FIRST_REPLY_RECEIVED);
			}
			else if (task.getStatus() == TaskStatus.CONFIRMATION_SENT) {
				task.setStatus(TaskStatus.SECOND_REPLY_RECEIVED);
			}
		}
		else {
			// 负面回复，如果有建议时间则更新
			if (response.getProposedTime() != null) {
				task.setScheduledTime(response.getProposedTime());
				task.setFinalConfirmedTime(response.getProposedTime()
					.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
				task.setStatus(TaskStatus.FIRST_REPLY_RECEIVED);
			}
		}
	}

	private void resumeWorkflow(WorkflowExecution execution, Task task, EmailResponse response) {
		try {
			logger.info("Resuming workflow for execution: {} with new email response", execution.getId());

			// 构建工作流状态
			WorkflowState state = new WorkflowState();
			state.setTask(task);
			state.setLatestResponse(response);
			state.setCurrentStep("WAIT_FOR_REPLY");
			state.setRetryCount(execution.getRetryCount());
			state.setLastEmailId(execution.getLastEmailId());

			// 准备恢复输入
			Map<String, Object> resumeInput = Map.of("task", task, "currentStep", "WAIT_FOR_REPLY", "lastEmailId",
					execution.getLastEmailId(), "latestResponse", response, "retryCount", execution.getRetryCount());

			// 恢复工作流执行
			Optional<WorkflowState> result = taskWorkflow.resume(execution.getThreadId(), resumeInput);

			if (result.isPresent()) {
				WorkflowState newState = result.get();
				logger.info("Workflow resumed successfully for execution: {}, new step: {}", execution.getId(),
						newState.getCurrentStep());

				// 更新执行状态
				if (newState.isCompleted()) {
					workflowExecutionService.updateExecutionStatus(execution.getId(),
							WorkflowExecution.WorkflowStatus.COMPLETED);
				}
				else {
					workflowExecutionService.updateExecutionStep(execution.getId(), newState.getCurrentStep());

					// 如果还是在等待状态，更新为等待回复
					if ("WAIT_FOR_REPLY".equals(newState.getCurrentStep())) {
						workflowExecutionService.updateExecutionStatus(execution.getId(),
								WorkflowExecution.WorkflowStatus.WAITING_FOR_REPLY);
					}
					else {
						workflowExecutionService.updateExecutionStatus(execution.getId(),
								WorkflowExecution.WorkflowStatus.RUNNING);
					}
				}
			}
			else {
				logger.warn("Workflow resume returned no result for execution: {}", execution.getId());
				workflowExecutionService.incrementRetryCount(execution.getId());
			}

		}
		catch (Exception e) {
			logger.error("Failed to resume workflow for execution: {}", execution.getId(), e);
			workflowExecutionService.incrementRetryCount(execution.getId());
		}
	}

}