package io.eeaters.statemachine.state.actions;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.model.EmailTask;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 完成任务动作
 *
 * 在状态转换到 COMPLETED 或 FAILED 时执行 负责清理资源、记录结果和更新任务状态
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompleteTaskAction
		implements Action<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> {

	private final TaskService taskService;

	private final CoordinatorAgent coordinatorAgent;

	@Override
	public void execute(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		log.info("执行完成任务动作");

		try {
			// 获取协调上下文
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null) {
				log.warn("协调上下文不存在，跳过任务完成处理");
				return;
			}

			// 判断完成状态
			boolean isSuccess = io.eeaters.statemachine.state.States.COMPLETED.equals(context.getTarget().getId());

			// 处理任务完成
			coordinatorAgent.handleTaskCompletion(coordinatorContext, isSuccess);

			// 生成任务完成报告
			TaskCompletionReport report = generateCompletionReport(coordinatorContext, isSuccess);

			// 保存报告到上下文
			coordinatorContext.setExtendedAttribute("completionReport", report);

			// 更新状态机变量
			context.getExtendedState().getVariables().put("currentAction", "COMPLETE_TASK");
			context.getExtendedState().getVariables().put("taskCompleted", true);
			context.getExtendedState().getVariables().put("completionSuccess", isSuccess);
			context.getExtendedState().getVariables().put("completionReport", report);

			// 记录完成日志
			log.info("任务完成处理成功: taskId={}, success={}, duration={}, totalEmails={}, totalPolls={}",
					coordinatorContext.getCurrentTaskId(), isSuccess, coordinatorContext.getExecutionDurationSeconds(),
					coordinatorContext.getSentEmailCount(), coordinatorContext.getPollCount());

			// 如果是成功完成，发送通知（这里可以扩展为邮件通知、webhook等）
			if (isSuccess) {
				sendCompletionNotification(coordinatorContext, report);
			}

			// 清理上下文（可选，根据需求决定是否保留）
			// cleanupContext(coordinatorContext);

		}
		catch (Exception e) {
			log.error("完成任务动作执行失败: {}", e.getMessage(), e);

			// 设置状态机错误信息
			context.getExtendedState().getVariables().put("error", e.getMessage());
			context.getExtendedState().getVariables().put("currentAction", "COMPLETE_TASK_FAILED");

			throw new RuntimeException("完成任务失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 生成任务完成报告
	 */
	private TaskCompletionReport generateCompletionReport(CoordinatorContext coordinatorContext, boolean isSuccess) {
		EmailTask task = coordinatorContext.getCurrentTask();

		TaskCompletionReport report = new TaskCompletionReport();
		report.setTaskId(coordinatorContext.getCurrentTaskId());
		report.setTaskTitle(task != null ? task.getTitle() : "未知任务");
		report.setTargetEmail(task != null ? task.getTargetEmail() : "未知邮箱");
		report.setSuccess(isSuccess);
		report.setStartTime(coordinatorContext.getStartTime());
		report.setEndTime(LocalDateTime.now());
		report.setDurationSeconds(coordinatorContext.getExecutionDurationSeconds());
		report.setTotalEmailsSent(coordinatorContext.getSentEmailCount());
		report.setTotalPolls(coordinatorContext.getPollCount());
		report.setFinalState(coordinatorContext.getCurrentState());

		// 设置完成原因
		if (isSuccess) {
			if (coordinatorContext.getCurrentResponse() != null) {
				report.setCompletionReason("收到"
						+ getResponseTypeDescription(coordinatorContext.getCurrentResponse().getResponseType()) + "响应");
			}
			else {
				report.setCompletionReason("任务正常完成");
			}
		}
		else {
			report.setCompletionReason(
					coordinatorContext.getErrorMessage() != null ? coordinatorContext.getErrorMessage() : "任务失败");
		}

		// 设置响应信息
		if (coordinatorContext.getCurrentResponse() != null) {
			report.setResponseType(coordinatorContext.getCurrentResponse().getResponseType());
			report.setResponseConfidence(coordinatorContext.getCurrentResponse().getConfidence());
			report.setResponseReceivedAt(coordinatorContext.getCurrentResponse().getReceivedAt());
		}

		// 设置错误信息
		if (coordinatorContext.hasError()) {
			report.setError(coordinatorContext.getErrorMessage());
			report.setErrorTime(coordinatorContext.getLastErrorTime());
		}

		return report;
	}

	/**
	 * 发送完成通知
	 */
	private void sendCompletionNotification(CoordinatorContext coordinatorContext, TaskCompletionReport report) {
		try {
			log.info("发送任务完成通知: taskId={}, success={}", report.getTaskId(), report.isSuccess());

			// 这里可以实现各种通知方式：
			// 1. 邮件通知
			// 2. Webhook通知
			// 3. 数据库记录
			// 4. 日志记录

			// 目前只是记录日志
			if (report.isSuccess()) {
				log.info("🎉 任务完成通知: {} - {}", report.getTaskTitle(), report.getCompletionReason());
			}
			else {
				log.error("❌ 任务失败通知: {} - {}", report.getTaskTitle(), report.getCompletionReason());
			}

		}
		catch (Exception e) {
			log.error("发送完成通知失败: {}", e.getMessage(), e);
		}
	}

	/**
	 * 清理上下文资源
	 */
	private void cleanupContext(CoordinatorContext coordinatorContext) {
		log.info("清理任务上下文资源: taskId={}", coordinatorContext.getCurrentTaskId());

		// 清理临时数据
		coordinatorContext.clearTemporaryData();

		// 清理敏感信息
		coordinatorContext.setErrorMessage(null);
		coordinatorContext.setLastErrorTime(null);

		// 可以根据需求决定是否完全清理上下文
		// coordinatorContext.setCurrentTask(null);
		// coordinatorContext.setCurrentResponse(null);
	}

	/**
	 * 获取响应类型描述
	 */
	private String getResponseTypeDescription(io.eeaters.statemachine.model.EmailResponse.ResponseType responseType) {
		if (responseType == null) {
			return "未知";
		}
		return switch (responseType) {
			case AGREE -> "同意";
			case DISAGREE -> "不同意";
			case UNCLEAR -> "不明确";
			case NEED_INFO -> "需要信息";
			case NOT_RELATED -> "不相关";
		};
	}

	/**
	 * 从上下文中获取协调上下文
	 */
	private CoordinatorContext getCoordinatorContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		return (CoordinatorContext) context.getExtendedState().getVariables().get("coordinatorContext");
	}

	/**
	 * 任务完成报告
	 */
	public static class TaskCompletionReport {

		private String taskId;

		private String taskTitle;

		private String targetEmail;

		private boolean success;

		private LocalDateTime startTime;

		private LocalDateTime endTime;

		private long durationSeconds;

		private Integer totalEmailsSent;

		private Integer totalPolls;

		private io.eeaters.statemachine.state.States finalState;

		private String completionReason;

		private io.eeaters.statemachine.model.EmailResponse.ResponseType responseType;

		private Double responseConfidence;

		private LocalDateTime responseReceivedAt;

		private String error;

		private LocalDateTime errorTime;

		// Getters and Setters
		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public String getTaskTitle() {
			return taskTitle;
		}

		public void setTaskTitle(String taskTitle) {
			this.taskTitle = taskTitle;
		}

		public String getTargetEmail() {
			return targetEmail;
		}

		public void setTargetEmail(String targetEmail) {
			this.targetEmail = targetEmail;
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public LocalDateTime getStartTime() {
			return startTime;
		}

		public void setStartTime(LocalDateTime startTime) {
			this.startTime = startTime;
		}

		public LocalDateTime getEndTime() {
			return endTime;
		}

		public void setEndTime(LocalDateTime endTime) {
			this.endTime = endTime;
		}

		public long getDurationSeconds() {
			return durationSeconds;
		}

		public void setDurationSeconds(long durationSeconds) {
			this.durationSeconds = durationSeconds;
		}

		public Integer getTotalEmailsSent() {
			return totalEmailsSent;
		}

		public void setTotalEmailsSent(Integer totalEmailsSent) {
			this.totalEmailsSent = totalEmailsSent;
		}

		public Integer getTotalPolls() {
			return totalPolls;
		}

		public void setTotalPolls(Integer totalPolls) {
			this.totalPolls = totalPolls;
		}

		public io.eeaters.statemachine.state.States getFinalState() {
			return finalState;
		}

		public void setFinalState(io.eeaters.statemachine.state.States finalState) {
			this.finalState = finalState;
		}

		public String getCompletionReason() {
			return completionReason;
		}

		public void setCompletionReason(String completionReason) {
			this.completionReason = completionReason;
		}

		public io.eeaters.statemachine.model.EmailResponse.ResponseType getResponseType() {
			return responseType;
		}

		public void setResponseType(io.eeaters.statemachine.model.EmailResponse.ResponseType responseType) {
			this.responseType = responseType;
		}

		public Double getResponseConfidence() {
			return responseConfidence;
		}

		public void setResponseConfidence(Double responseConfidence) {
			this.responseConfidence = responseConfidence;
		}

		public LocalDateTime getResponseReceivedAt() {
			return responseReceivedAt;
		}

		public void setResponseReceivedAt(LocalDateTime responseReceivedAt) {
			this.responseReceivedAt = responseReceivedAt;
		}

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}

		public LocalDateTime getErrorTime() {
			return errorTime;
		}

		public void setErrorTime(LocalDateTime errorTime) {
			this.errorTime = errorTime;
		}

		@Override
		public String toString() {
			return String.format(
					"TaskCompletionReport{taskId='%s', title='%s', success=%s, duration=%ds, emails=%d, polls=%d}",
					taskId, taskTitle, success, durationSeconds, totalEmailsSent, totalPolls);
		}

	}

}