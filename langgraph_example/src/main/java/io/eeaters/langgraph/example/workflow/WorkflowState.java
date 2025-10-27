package io.eeaters.langgraph.example.workflow;

import io.eeaters.langgraph.example.model.EmailResponse;
import io.eeaters.langgraph.example.model.Task;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;

public class WorkflowState extends AgentState {

	public WorkflowState() {
		super(Map.of());
	}

	public WorkflowState(Map<String, Object> data) {
		super(data);
	}

	public Task getTask() {
		return this.<Task>value("task").orElse(null);
	}

	public void setTask(Task task) {
		this.data().put("task", task);
	}

	public String getCurrentStep() {
		return this.<String>value("currentStep").orElse("");
	}

	public void setCurrentStep(String currentStep) {
		this.data().put("currentStep", currentStep);
	}

	public String getLastEmailId() {
		return this.<String>value("lastEmailId").orElse(null);
	}

	public void setLastEmailId(String lastEmailId) {
		this.data().put("lastEmailId", lastEmailId);
	}

	public EmailResponse getLatestResponse() {
		return this.<EmailResponse>value("latestResponse").orElse(null);
	}

	public void setLatestResponse(EmailResponse latestResponse) {
		this.data().put("latestResponse", latestResponse);
	}

	public int getRetryCount() {
		return this.<Integer>value("retryCount").orElse(0);
	}

	public void setRetryCount(int retryCount) {
		this.data().put("retryCount", retryCount);
	}

	public boolean isCompleted() {
		return this.<Boolean>value("completed").orElse(false);
	}

	public void setCompleted(boolean completed) {
		this.data().put("completed", completed);
	}

	public boolean isTerminated() {
		return this.<Boolean>value("terminated").orElse(false);
	}

	public void setTerminated(boolean terminated) {
		this.data().put("terminated", terminated);
	}

	public Map<String, Object> toData() {
		return Map.of("task", getTask(), "currentStep", getCurrentStep(), "lastEmailId", getLastEmailId(),
				"latestResponse", getLatestResponse(), "retryCount", getRetryCount(), "completed", isCompleted(),
				"terminated", isTerminated());
	}

}