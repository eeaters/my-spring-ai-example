package io.eeaters.langgraph.example.workflow;

import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.service.ConfigService;
import io.eeaters.langgraph.example.workflow.nodes.*;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.StateGraph.END;

@Component
public class TaskWorkflow {

	private static final Logger logger = LoggerFactory.getLogger(TaskWorkflow.class);

	public static final Map<String, Channel<?>> SCHEMA = Map.of("task", Channels.base(() -> null), "currentStep",
			Channels.base(() -> ""), "lastEmailId", Channels.base(() -> ""), "latestResponse",
			Channels.base(() -> null), "retryCount", Channels.base(() -> 0), "completed", Channels.base(() -> false),
			"terminated", Channels.base(() -> false));

	@Autowired
	private CreateTaskNode createTaskNode;

	@Autowired
	private SendAutoReplyNode sendAutoReplyNode;

	@Autowired
	private WaitForReplyNode waitForReplyNode;

	@Autowired
	private SendConfirmationNode sendConfirmationNode;

	@Autowired
	private SendFinalAgreementNode sendFinalAgreementNode;

	@Autowired
	private ConfigService configService;

	private CompiledGraph<WorkflowState> compiledGraph;

	public void initialize() throws org.bsc.langgraph4j.GraphStateException {
		if (!configService.validateConfiguration()) {
			throw new IllegalStateException("Configuration validation failed");
		}

		StateGraph<WorkflowState> workflow = new StateGraph<>(SCHEMA, WorkflowState::new)
			.addNode("CREATE_TASK", node_async(createTaskNode.createTask()))
			.addNode("SEND_AUTO_REPLY", node_async(sendAutoReplyNode.sendAutoReply()))
			.addNode("WAIT_FOR_REPLY", node_async(waitForReplyNode.waitForReply()))
			.addNode("SEND_CONFIRMATION", node_async(sendConfirmationNode.sendConfirmation()))
			.addNode("SEND_FINAL_AGREEMENT", node_async(sendFinalAgreementNode.sendFinalAgreement()))
			.addNode("COMPLETED", node_async(this::completeTask))
			.addNode("TERMINATED", node_async(this::terminateTask))

			.addEdge(START, "CREATE_TASK")
			.addEdge("CREATE_TASK", "SEND_AUTO_REPLY")
			.addEdge("SEND_AUTO_REPLY", "WAIT_FOR_REPLY")
			.addEdge("SEND_CONFIRMATION", "WAIT_FOR_REPLY")
			.addEdge("SEND_FINAL_AGREEMENT", "COMPLETED")
			.addEdge("COMPLETED", END)
			.addEdge("TERMINATED", END);

		workflow.addConditionalEdges("WAIT_FOR_REPLY", edge_async(this::decideNextStep),
				Map.of("SEND_CONFIRMATION", "SEND_CONFIRMATION",
                        "SEND_AUTO_REPLY", "SEND_AUTO_REPLY",
                        "WAIT_FOR_REPLY", "WAIT_FOR_REPLY",
                        "SEND_FINAL_AGREEMENT", "SEND_FINAL_AGREEMENT",
                        "TERMINATED", "TERMINATED"));

		CompileConfig compileConfig = CompileConfig.builder()
			.checkpointSaver(new MemorySaver())
			.interruptAfter("WAIT_FOR_REPLY")
			.releaseThread(true)
			.build();

		this.compiledGraph = workflow.compile(compileConfig);

		GraphRepresentation graph = compiledGraph.getGraph(GraphRepresentation.Type.MERMAID);
		logger.info("Workflow graph:\n{}", graph.content());
	}

	private Map<String, Object> completeTask(WorkflowState state) {
		logger.info("Task completed: {}", state.getTask().getId());
		state.setCompleted(true);
		return state.toData();
	}

	private Map<String, Object> terminateTask(WorkflowState state) {
		logger.warn("Task terminated: {}", state.getTask().getId());
		state.setTerminated(true);
		return state.toData();
	}

	private String decideNextStep(WorkflowState state) {
		Task task = state.getTask();
		String currentStep = state.getCurrentStep();
		int retryCount = state.getRetryCount();

		if (retryCount >= configService.getMaxRetryAttempts()) {
			logger.warn("Max retry attempts reached for task: {}", task.getId());
			return "TERMINATED";
		}

		if (state.getLatestResponse() == null) {
			logger.info("No response received, continue waiting for task: {}", task.getId());
			return "WAIT_FOR_REPLY";
		}

		boolean isPositive = state.getLatestResponse().isPositive();
		TaskStatus status = task.getStatus();

		logger.info("Decision for task {}: isPositive={}, status={}, retryCount={}", task.getId(), isPositive, status,
				retryCount);

		if (isPositive) {
			if (status == TaskStatus.FIRST_REPLY_RECEIVED) {
				return "SEND_CONFIRMATION";
			}
			else if (status == TaskStatus.SECOND_REPLY_RECEIVED) {
				return "SEND_FINAL_AGREEMENT";
			}
		}
		else {
			if (state.getLatestResponse().getProposedTime() != null) {
				return "SEND_AUTO_REPLY";
			}
		}

		return "WAIT_FOR_REPLY";
	}

	public Optional<WorkflowState> execute(Task task) {
		if (compiledGraph == null) {
			try {
				initialize();
			}
			catch (org.bsc.langgraph4j.GraphStateException e) {
				throw new RuntimeException("Failed to initialize workflow", e);
			}
		}

		logger.info("Executing workflow for task: {}", task.getId());

		try {
			Optional<WorkflowState> result = compiledGraph.invoke(Map.of("task", task));
			logger.info("Workflow execution completed for task: {}", task.getId());
			return result;
		}
		catch (Exception e) {
			logger.error("Workflow execution failed for task: {}", task.getId(), e);
			throw e;
		}
	}

	public Optional<WorkflowState> resume(String threadId, Map<String, Object> input) {
		if (compiledGraph == null) {
			try {
				initialize();
			}
			catch (org.bsc.langgraph4j.GraphStateException e) {
				throw new RuntimeException("Failed to initialize workflow", e);
			}
		}

		logger.info("Resuming workflow for thread: {}", threadId);

		try {
			Optional<WorkflowState> result = compiledGraph.invoke(input,
					org.bsc.langgraph4j.RunnableConfig.builder().threadId(threadId).build());
			logger.info("Workflow resumed successfully for thread: {}", threadId);
			return result;
		}
		catch (Exception e) {
			logger.error("Failed to resume workflow for thread: {}", threadId, e);
			throw e;
		}
	}

	public GraphRepresentation getGraphRepresentation() {
		if (compiledGraph == null) {
			try {
				initialize();
			}
			catch (org.bsc.langgraph4j.GraphStateException e) {
				throw new RuntimeException("Failed to initialize workflow", e);
			}
		}
		return compiledGraph.getGraph(GraphRepresentation.Type.MERMAID);
	}

	public CompiledGraph<WorkflowState> getCompiledGraph() {
		if (compiledGraph == null) {
			try {
				initialize();
			}
			catch (org.bsc.langgraph4j.GraphStateException e) {
				throw new RuntimeException("Failed to initialize workflow", e);
			}
		}
		return compiledGraph;
	}

}