package io.eeaters.statemachine.agent;

import io.eeaters.statemachine.model.EmailTask;
import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 协调代理
 *
 * 负责整体流程协调和决策 管理状态机上下文，协调各个组件的工作
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoordinatorAgent {

	private final TaskService taskService;

	private final ResponseAnalyzerAgent responseAnalyzerAgent;

	/**
	 * 创建协调上下文
	 * @param taskId 任务ID
	 * @return 协调上下文
	 */
	public CoordinatorContext createCoordinatorContext(String taskId) {
		log.info("创建协调上下文: taskId={}", taskId);

		EmailTask task = taskService.getTask(taskId);
		if (task == null) {
			throw new IllegalArgumentException("任务不存在: " + taskId);
		}

		CoordinatorContext context = new CoordinatorContext(taskId);
		context.setCurrentTask(task);
		context.setCurrentTaskId(taskId);
		context.setStateMachineId(generateStateMachineId(taskId));

		// 设置决策变量
		context.setDecisionVariable("maxPollCount", 100);
		context.setDecisionVariable("pollInterval", 30000);
		context.setDecisionVariable("analysisThreshold", 0.7);

		// 更新任务状态为运行中
		taskService.updateTaskStatus(taskId, EmailTask.TaskStatus.RUNNING);

		log.info("协调上下文创建完成: taskId={}, stateMachineId={}", taskId, context.getStateMachineId());
		return context;
	}

	/**
	 * 更新上下文状态
	 * @param context 协调上下文
	 * @param newState 新状态
	 */
	public void updateContextState(CoordinatorContext context, io.eeaters.statemachine.state.States newState) {
		log.info("更新上下文状态: taskId={}, {} -> {}", context.getCurrentTaskId(), context.getCurrentState(), newState);

		context.setCurrentState(newState);
		context.setExtendedAttribute("lastStateChange", LocalDateTime.now());

		// 根据状态更新任务状态
		updateTaskStatusByState(context, newState);
	}

	/**
	 * 设置任务到上下文
	 * @param context 协调上下文
	 * @param task 邮件任务
	 */
	public void setTaskToContext(CoordinatorContext context, EmailTask task) {
		log.info("设置任务到上下文: taskId={}, taskTitle={}", task.getTaskId(), task.getTitle());

		context.setCurrentTask(task);
		context.setCurrentTaskId(task.getTaskId());
		context.setExtendedAttribute("taskSetTime", LocalDateTime.now());
	}

	/**
	 * 设置响应到上下文
	 * @param context 协调上下文
	 * @param response 邮件响应
	 */
	public void setResponseToContext(CoordinatorContext context, io.eeaters.statemachine.model.EmailResponse response) {
		log.info("设置响应到上下文: taskId={}, responseType={}, confidence={}", response.getTaskId(),
				response.getResponseType(), response.getConfidence());

		context.setCurrentResponse(response);
		context.setExtendedAttribute("responseSetTime", LocalDateTime.now());
		context.setDecisionVariable("responseType", response.getResponseType());
		context.setDecisionVariable("responseConfidence", response.getConfidence());
	}

	/**
	 * 检查是否应该继续轮询
	 * @param context 协调上下文
	 * @return true如果应该继续轮询
	 */
	public boolean shouldContinuePolling(CoordinatorContext context) {
		// 检查任务是否超时
		if (context.getCurrentTask() != null && context.getCurrentTask().isTimeout()) {
			log.info("任务已超时，停止轮询: taskId={}", context.getCurrentTaskId());
			return false;
		}

		// 检查轮询次数限制
		Integer maxPollCount = context.getDecisionVariable("maxPollCount");
		if (maxPollCount != null && context.getPollCount() >= maxPollCount) {
			log.info("轮询次数达到上限，停止轮询: taskId={}, currentCount={}, maxCount={}", context.getCurrentTaskId(),
					context.getPollCount(), maxPollCount);
			return false;
		}

		// 检查是否有错误
		if (context.hasError() && !context.getCurrentTask().canRetry()) {
			log.info("任务错误且无法重试，停止轮询: taskId={}", context.getCurrentTaskId());
			return false;
		}

		return true;
	}

	/**
	 * 处理任务完成
	 * @param context 协调上下文
	 * @param success 是否成功完成
	 */
	public void handleTaskCompletion(CoordinatorContext context, boolean success) {
		String taskId = context.getCurrentTaskId();
		log.info("处理任务完成: taskId={}, success={}", taskId, success);

		if (success) {
			taskService.updateTaskStatus(taskId, EmailTask.TaskStatus.COMPLETED);
			context.setExtendedAttribute("completionTime", LocalDateTime.now());
			context.setExtendedAttribute("completionStatus", "SUCCESS");
		}
		else {
			taskService.updateTaskStatus(taskId, EmailTask.TaskStatus.FAILED);
			context.setExtendedAttribute("completionTime", LocalDateTime.now());
			context.setExtendedAttribute("completionStatus", "FAILED");
		}

		// 清理临时数据
		context.clearTemporaryData();
	}

	/**
	 * 处理任务错误
	 * @param context 协调上下文
	 * @param errorMessage 错误信息
	 */
	public void handleTaskError(CoordinatorContext context, String errorMessage) {
		log.error("处理任务错误: taskId={}, error={}", context.getCurrentTaskId(), errorMessage);

		context.setError(errorMessage);

		// 增加重试次数
		if (context.getCurrentTask() != null) {
			context.getCurrentTask().incrementRetry();
		}

		// 设置错误决策变量
		context.setDecisionVariable("hasError", true);
		context.setDecisionVariable("canRetry",
				context.getCurrentTask() != null && context.getCurrentTask().canRetry());
	}

	/**
	 * 获取上下文摘要信息
	 * @param context 协调上下文
	 * @return 摘要信息
	 */
	public Map<String, Object> getContextSummary(CoordinatorContext context) {
		Map<String, Object> summary = new HashMap<>();

		summary.put("taskId", context.getCurrentTaskId());
		summary.put("stateMachineId", context.getStateMachineId());
		summary.put("currentState", context.getCurrentState());
		summary.put("startTime", context.getStartTime());
		summary.put("lastUpdateTime", context.getLastUpdateTime());
		summary.put("pollCount", context.getPollCount());
		summary.put("sentEmailCount", context.getSentEmailCount());
		summary.put("hasError", context.hasError());
		summary.put("executionDuration", context.getExecutionDurationSeconds());

		if (context.getCurrentTask() != null) {
			summary.put("taskTitle", context.getCurrentTask().getTitle());
			summary.put("taskStatus", context.getCurrentTask().getStatus());
			summary.put("targetEmail", context.getCurrentTask().getTargetEmail());
		}

		if (context.getCurrentResponse() != null) {
			summary.put("responseType", context.getCurrentResponse().getResponseType());
			summary.put("responseConfidence", context.getCurrentResponse().getConfidence());
		}

		return summary;
	}

	/**
	 * 根据状态更新任务状态
	 */
	private void updateTaskStatusByState(CoordinatorContext context, io.eeaters.statemachine.state.States state) {
		EmailTask.TaskStatus taskStatus = switch (state) {
			case TASK_CREATED -> EmailTask.TaskStatus.PENDING;
			case EMAIL_SENT, POLLING, SECOND_EMAIL_SENT -> EmailTask.TaskStatus.RUNNING;
			case COMPLETED -> EmailTask.TaskStatus.COMPLETED;
			case FAILED -> EmailTask.TaskStatus.FAILED;
			default ->
				context.getCurrentTask() != null ? context.getCurrentTask().getStatus() : EmailTask.TaskStatus.PENDING;
		};

		if (context.getCurrentTask() != null && !context.getCurrentTask().getStatus().equals(taskStatus)) {
			taskService.updateTaskStatus(context.getCurrentTaskId(), taskStatus);
		}
	}

	/**
	 * 生成状态机ID
	 */
	private String generateStateMachineId(String taskId) {
		return "sm-" + taskId + "-" + System.currentTimeMillis();
	}

}