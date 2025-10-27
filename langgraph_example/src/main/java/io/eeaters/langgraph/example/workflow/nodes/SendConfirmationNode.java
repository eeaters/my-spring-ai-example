package io.eeaters.langgraph.example.workflow.nodes;

import io.eeaters.langgraph.example.config.WorkflowConfig;
import io.eeaters.langgraph.example.model.Party;
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
public class SendConfirmationNode {

	private static final Logger logger = LoggerFactory.getLogger(SendConfirmationNode.class);

	@Autowired
	private EmailService emailService;

	@Autowired
	private WorkflowConfig workflowConfig;

	@Autowired
	private EmailTemplate emailTemplate;

	public NodeAction<WorkflowState> sendConfirmation() {
		return state -> {
			logger.info("Sending confirmation email");

			Task task = state.getTask();
			if (task == null) {
				throw new IllegalArgumentException("Task cannot be null");
			}

			Party nextParty = task.getNextParty();
			String recipientEmail = getRecipientEmail(nextParty);
			String subject = "任务安排确认 - " + task.getTitle();
			String content = emailTemplate.generateConfirmation(task, recipientEmail);

			String emailId = emailService.sendEmail(recipientEmail, subject, content);

			task.setCurrentParty(nextParty);
			task.setEmailThreadId(emailId);
			task.setStatus(TaskStatus.CONFIRMATION_SENT);

			logger.info("Confirmation sent successfully to: {}, emailId: {}", recipientEmail, emailId);

			WorkflowState newState = new WorkflowState();
			newState.setTask(task);
			newState.setCurrentStep("WAIT_FOR_SECOND_REPLY");
			newState.setLastEmailId(emailId);
			newState.setRetryCount(0);

			return newState.toData();
		};
	}

	private String getRecipientEmail(Party party) {
		return switch (party) {
			case TRAILER_COMPANY -> workflowConfig.getTrailerCompanyEmail();
			case WAREHOUSE -> workflowConfig.getWarehouseEmail();
		};
	}

}