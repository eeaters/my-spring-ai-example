package io.eeaters.langgraph.example.workflow.nodes;

import io.eeaters.langgraph.example.config.WorkflowConfig;
import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.service.EmailService;
import io.eeaters.langgraph.example.util.EmailTemplate;
import io.eeaters.langgraph.example.workflow.WorkflowState;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class SendAutoReplyNode {

	private static final Logger logger = LoggerFactory.getLogger(SendAutoReplyNode.class);

	@Autowired
	private EmailService emailService;

	@Autowired
	private WorkflowConfig workflowConfig;

	@Autowired
	private EmailTemplate emailTemplate;

	public NodeAction<WorkflowState> sendAutoReply() {
		return state -> {
			logger.info("Sending auto reply email");

			Task task = state.getTask();
			if (task == null) {
				throw new IllegalArgumentException("Task cannot be null");
			}

			String recipientEmail = getRecipientEmail(task.getCurrentParty());
			String subject = "任务安排协调 - " + task.getTitle();
			String content = emailTemplate.generateAutoReply(task, recipientEmail);

			String emailId = emailService.sendEmail(recipientEmail, subject, content);

			task.setEmailThreadId(emailId);
			task.setStatus(TaskStatus.AUTO_REPLY_SENT);

			logger.info("Auto reply sent successfully to: {}, emailId: {}", recipientEmail, emailId);

			WorkflowState newState = new WorkflowState();
			newState.setTask(task);
			newState.setCurrentStep("WAIT_FOR_REPLY");
			newState.setLastEmailId(emailId);
			newState.setRetryCount(0);

			return newState.toData();
		};
	}

	private String getRecipientEmail(io.eeaters.langgraph.example.model.Party party) {
		return switch (party) {
			case TRAILER_COMPANY -> workflowConfig.getTrailerCompanyEmail();
			case WAREHOUSE -> workflowConfig.getWarehouseEmail();
		};
	}

}