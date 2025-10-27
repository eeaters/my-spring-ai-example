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
public class SendFinalAgreementNode {

	private static final Logger logger = LoggerFactory.getLogger(SendFinalAgreementNode.class);

	@Autowired
	private EmailService emailService;

	@Autowired
	private WorkflowConfig workflowConfig;

	@Autowired
	private EmailTemplate emailTemplate;

	public NodeAction<WorkflowState> sendFinalAgreement() {
		return state -> {
			logger.info("Sending final agreement emails");

			Task task = state.getTask();
			if (task == null) {
				throw new IllegalArgumentException("Task cannot be null");
			}

			String subject = "最终任务安排确认 - " + task.getTitle();
			String content = emailTemplate.generateFinalAgreement(task, "");

			String trailerEmail = workflowConfig.getTrailerCompanyEmail();
			String warehouseEmail = workflowConfig.getWarehouseEmail();

			emailService.sendEmail(trailerEmail, subject, content);
			emailService.sendEmail(warehouseEmail, subject, content);

			task.setStatus(TaskStatus.FINAL_AGREEMENT_SENT);
			task.setStatus(TaskStatus.COMPLETED);

			logger.info("Final agreement sent to both parties, task completed: {}", task.getId());

			WorkflowState newState = new WorkflowState();
			newState.setTask(task);
			newState.setCurrentStep("COMPLETED");
			newState.setCompleted(true);

			return newState.toData();
		};
	}

}