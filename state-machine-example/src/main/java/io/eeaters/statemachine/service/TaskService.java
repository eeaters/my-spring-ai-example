package io.eeaters.statemachine.service;

import io.eeaters.statemachine.config.EmailConfig;
import io.eeaters.statemachine.model.EmailTask;
import io.eeaters.statemachine.model.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务服务类
 *
 * 负责邮件任务的创建、管理、存储和查询 使用内存存储，实际项目中可以替换为数据库
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

	private final EmailConfig emailConfig;

	// 内存存储，实际项目中应该使用数据库
	private final ConcurrentMap<String, EmailTask> taskStore = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, EmailResponse> responseStore = new ConcurrentHashMap<>();

	/**
	 * 创建新的邮件协调任务
	 * @param title 任务标题
	 * @param description 任务描述
	 * @param targetEmail 目标邮箱
	 * @param subject 邮件主题
	 * @param body 邮件内容
	 * @return 创建的邮件任务
	 */
	public EmailTask createTask(String title, String description, String targetEmail, String subject, String body) {
		log.info("创建邮件协调任务: title={}, targetEmail={}", title, targetEmail);

		EmailTask task = EmailTask.builder()
			.taskId(UUID.randomUUID().toString())
			.title(title)
			.description(description)
			.targetEmail(targetEmail)
			.fromEmail(getFromEmail())
			.initialSubject(subject)
			.initialBody(body)
			.followUpSubject(emailConfig.getTemplates().getFollowUpEmail().getSubject())
			.followUpBody(emailConfig.getTemplates().getFollowUpEmail().getBody())
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.deadline(LocalDateTime.now().plusSeconds(emailConfig.getTask().getTimeout() / 1000))
			.status(EmailTask.TaskStatus.PENDING)
			.maxRetries(emailConfig.getTask().getRetryCount())
			.currentRetries(0)
			.build();

		taskStore.put(task.getTaskId(), task);

		log.info("邮件任务创建成功: taskId={}, targetEmail={}", task.getTaskId(), targetEmail);
		return task;
	}

	/**
	 * 根据任务ID获取任务
	 * @param taskId 任务ID
	 * @return 邮件任务，如果不存在则返回null
	 */
	public EmailTask getTask(String taskId) {
		return taskStore.get(taskId);
	}

	/**
	 * 更新任务状态
	 * @param taskId 任务ID
	 * @param status 新状态
	 */
	public void updateTaskStatus(String taskId, EmailTask.TaskStatus status) {
		EmailTask task = taskStore.get(taskId);
		if (task != null) {
			task.setStatus(status);
			task.setUpdatedAt(LocalDateTime.now());
			log.info("任务状态更新: taskId={}, status={}", taskId, status);
		}
	}

	/**
	 * 保存邮件响应
	 * @param response 邮件响应
	 */
	public void saveResponse(EmailResponse response) {
		responseStore.put(response.getResponseId(), response);
		log.info("邮件响应已保存: responseId={}, taskId={}, responseType={}", response.getResponseId(), response.getTaskId(),
				response.getResponseType());
	}

	/**
	 * 根据任务ID获取最新的邮件响应
	 * @param taskId 任务ID
	 * @return 最新的邮件响应，如果没有则返回null
	 */
	public EmailResponse getLatestResponse(String taskId) {
		return responseStore.values()
			.stream()
			.filter(response -> taskId.equals(response.getTaskId()))
			.max((r1, r2) -> r1.getReceivedAt().compareTo(r2.getReceivedAt()))
			.orElse(null);
	}

	/**
	 * 标记响应为已处理
	 * @param responseId 响应ID
	 */
	public void markResponseAsProcessed(String responseId) {
		EmailResponse response = responseStore.get(responseId);
		if (response != null) {
			response.markAsProcessed();
			log.info("邮件响应已标记为已处理: responseId={}", responseId);
		}
	}

	/**
	 * 检查任务是否存在
	 * @param taskId 任务ID
	 * @return true如果任务存在
	 */
	public boolean taskExists(String taskId) {
		return taskStore.containsKey(taskId);
	}

	/**
	 * 删除任务（包括相关的响应）
	 * @param taskId 任务ID
	 */
	public void deleteTask(String taskId) {
		taskStore.remove(taskId);

		// 删除相关的响应
		responseStore.entrySet().removeIf(entry -> taskId.equals(entry.getValue().getTaskId()));

		log.info("任务已删除: taskId={}", taskId);
	}

	/**
	 * 获取任务统计信息
	 * @return 任务统计信息
	 */
	public TaskStatistics getTaskStatistics() {
		int totalTasks = taskStore.size();
		int completedTasks = (int) taskStore.values()
			.stream()
			.filter(task -> EmailTask.TaskStatus.COMPLETED.equals(task.getStatus()))
			.count();
		int failedTasks = (int) taskStore.values()
			.stream()
			.filter(task -> EmailTask.TaskStatus.FAILED.equals(task.getStatus()))
			.count();
		int runningTasks = (int) taskStore.values()
			.stream()
			.filter(task -> EmailTask.TaskStatus.RUNNING.equals(task.getStatus()))
			.count();

		return new TaskStatistics(totalTasks, completedTasks, failedTasks, runningTasks);
	}

	/**
	 * 清理超时的任务
	 */
	public void cleanupTimeoutTasks() {
		LocalDateTime now = LocalDateTime.now();
		int cleanedCount = 0;

		for (EmailTask task : taskStore.values()) {
			if (task.isTimeout() && !EmailTask.TaskStatus.COMPLETED.equals(task.getStatus())) {
				task.setStatus(EmailTask.TaskStatus.TIMEOUT);
				task.setUpdatedAt(now);
				cleanedCount++;
			}
		}

		if (cleanedCount > 0) {
			log.info("清理超时任务完成，共清理 {} 个任务", cleanedCount);
		}
	}

	/**
	 * 获取发送方邮箱地址
	 */
	private String getFromEmail() {
		// 实际项目中应该从配置中获取
		return "system@eeaters.io";
	}

	/**
	 * 任务统计信息
	 */
	public static class TaskStatistics {

		private final int totalTasks;

		private final int completedTasks;

		private final int failedTasks;

		private final int runningTasks;

		public TaskStatistics(int totalTasks, int completedTasks, int failedTasks, int runningTasks) {
			this.totalTasks = totalTasks;
			this.completedTasks = completedTasks;
			this.failedTasks = failedTasks;
			this.runningTasks = runningTasks;
		}

		public int getTotalTasks() {
			return totalTasks;
		}

		public int getCompletedTasks() {
			return completedTasks;
		}

		public int getFailedTasks() {
			return failedTasks;
		}

		public int getRunningTasks() {
			return runningTasks;
		}

		public double getSuccessRate() {
			return totalTasks > 0 ? (double) completedTasks / totalTasks : 0.0;
		}

		@Override
		public String toString() {
			return String.format("TaskStatistics{total=%d, completed=%d, failed=%d, running=%d, successRate=%.2f%%}",
					totalTasks, completedTasks, failedTasks, runningTasks, getSuccessRate() * 100);
		}

	}

}