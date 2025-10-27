package io.eeaters.langgraph.example.workflow.nodes;

import io.eeaters.langgraph.example.config.WorkflowConfig;
import io.eeaters.langgraph.example.model.Party;
import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.service.TaskService;
import io.eeaters.langgraph.example.workflow.WorkflowState;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class CreateTaskNode {

	private static final Logger logger = LoggerFactory.getLogger(CreateTaskNode.class);

	@Autowired
	private TaskService taskService;

	@Autowired
	private WorkflowConfig workflowConfig;

	public NodeAction<WorkflowState> createTask() {
		return state -> {
			logger.info("Creating new task in workflow");

			Task task = state.getTask();
			if (task == null) {
				throw new IllegalArgumentException("Task cannot be null");
			}

			task.setStatus(TaskStatus.CREATED);
			task.setCurrentParty(workflowConfig.getFirstParty());
			task.setNextParty(workflowConfig.getSecondParty());

			Task savedTask = taskService.saveTask(task);

			logger.info("Task created successfully: {}", savedTask.getId());

			WorkflowState newState = new WorkflowState();
			newState.setTask(savedTask);
			newState.setCurrentStep("SEND_AUTO_REPLY");
			newState.setRetryCount(0);

			return newState.toData();
		};
	}

}