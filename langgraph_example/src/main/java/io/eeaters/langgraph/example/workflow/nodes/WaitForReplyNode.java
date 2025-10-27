package io.eeaters.langgraph.example.workflow.nodes;

import io.eeaters.langgraph.example.model.EmailResponse;
import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.model.WorkflowExecution;
import io.eeaters.langgraph.example.service.EmailService;
import io.eeaters.langgraph.example.service.WorkflowExecutionService;
import io.eeaters.langgraph.example.workflow.WorkflowState;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class WaitForReplyNode {

	private static final Logger logger = LoggerFactory.getLogger(WaitForReplyNode.class);

	@Autowired
	private EmailService emailService;

	@Autowired
	private WorkflowExecutionService workflowExecutionService;

	public NodeAction<WorkflowState> waitForReply() {
		return state -> {
			logger.info("Waiting for email reply");

			Task task = state.getTask();
			if (task == null) {
				throw new IllegalArgumentException("Task cannot be null");
			}

			String lastEmailId = state.getLastEmailId();
			List<EmailResponse> responses = emailService.checkNewEmails(lastEmailId);

			WorkflowState newState = new WorkflowState();
			newState.setTask(task);
			newState.setRetryCount(state.getRetryCount());

			if (responses.isEmpty()) {
				logger.info("No new emails received, persisting wait state for task: {}", task.getId());

				// 创建持久化的等待状态
				String threadId = java.util.UUID.randomUUID().toString();
				WorkflowExecution execution = workflowExecutionService.createWaitingExecution(task.getId(), threadId,
						lastEmailId);

				newState.setCurrentStep("WAIT_FOR_REPLY");
				newState.setRetryCount(state.getRetryCount() + 1);
				logger.info("Created waiting execution: {} for task: {}", execution.getId(), task.getId());
				return newState.toData();
			}

			EmailResponse latestResponse = responses.get(responses.size() - 1);
			newState.setLatestResponse(latestResponse);

			logger.info("Received email response: {}, isPositive: {}, proposedTime: {}", latestResponse.getSubject(),
					latestResponse.isPositive(), latestResponse.getProposedTime());

			Task updatedTask = task;
			if (latestResponse.isPositive()) {
				if (latestResponse.getProposedTime() != null) {
					task.setScheduledTime(latestResponse.getProposedTime());
					task.setFinalConfirmedTime(
							latestResponse.getProposedTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")));
				}

				task.setStatus(TaskStatus.FIRST_REPLY_RECEIVED);
				newState.setCurrentStep("SEND_CONFIRMATION");
			}
			else {
				if (latestResponse.getProposedTime() != null) {
					task.setScheduledTime(latestResponse.getProposedTime());
					task.setFinalConfirmedTime(
							latestResponse.getProposedTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")));
					task.setStatus(TaskStatus.FIRST_REPLY_RECEIVED);
					newState.setCurrentStep("SEND_AUTO_REPLY");
				}
				else {
					newState.setCurrentStep("WAIT_FOR_REPLY");
				}
			}

			newState.setTask(task);
			return newState.toData();
		};
	}

}