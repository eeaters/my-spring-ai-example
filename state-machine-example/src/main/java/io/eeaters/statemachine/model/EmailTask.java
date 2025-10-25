package io.eeaters.statemachine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 邮件任务模型
 *
 * 表示一个邮件协调任务的完整信息 包含任务的基本信息、目标邮箱、邮件内容等
 *
 * @author eeaters
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTask {

	/**
	 * 任务ID - 唯一标识一个邮件协调任务
	 */
	private String taskId;

	/**
	 * 任务标题 - 用于描述任务内容
	 */
	private String title;

	/**
	 * 任务描述 - 详细说明任务目的和要求
	 */
	private String description;

	/**
	 * 目标邮箱 - 邮件接收方的邮箱地址
	 */
	private String targetEmail;

	/**
	 * 发送方邮箱 - 邮件发送方的邮箱地址
	 */
	private String fromEmail;

	/**
	 * 初始邮件主题
	 */
	private String initialSubject;

	/**
	 * 初始邮件内容
	 */
	private String initialBody;

	/**
	 * 跟进邮件主题
	 */
	private String followUpSubject;

	/**
	 * 跟进邮件内容
	 */
	private String followUpBody;

	/**
	 * 任务创建时间
	 */
	private LocalDateTime createdAt;

	/**
	 * 任务更新时间
	 */
	private LocalDateTime updatedAt;

	/**
	 * 任务截止时间
	 */
	private LocalDateTime deadline;

	/**
	 * 任务状态
	 */
	private TaskStatus status;

	/**
	 * 最大重试次数
	 */
	private Integer maxRetries;

	/**
	 * 当前重试次数
	 */
	private Integer currentRetries;

	/**
	 * 任务扩展属性 - 存储额外的业务数据
	 */
	private Map<String, Object> attributes;

	/**
	 * 任务状态枚举
	 */
	public enum TaskStatus {

		PENDING, // 待处理
		RUNNING, // 运行中
		COMPLETED, // 已完成
		FAILED, // 失败
		TIMEOUT // 超时

	}

	/**
	 * 添加属性
	 * @param key 属性键
	 * @param value 属性值
	 */
	public void addAttribute(String key, Object value) {
		if (attributes == null) {
			attributes = new HashMap<>();
		}
		attributes.put(key, value);
	}

	/**
	 * 获取属性
	 * @param key 属性键
	 * @param <T> 属性值类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	public <T> T getAttribute(String key) {
		if (attributes == null) {
			return null;
		}
		return (T) attributes.get(key);
	}

	/**
	 * 检查任务是否超时
	 * @return true如果超时，false否则
	 */
	public boolean isTimeout() {
		return deadline != null && LocalDateTime.now().isAfter(deadline);
	}

	/**
	 * 检查是否可以重试
	 * @return true如果可以重试，false否则
	 */
	public boolean canRetry() {
		return maxRetries != null && currentRetries != null && currentRetries < maxRetries;
	}

	/**
	 * 增加重试次数
	 */
	public void incrementRetry() {
		if (currentRetries == null) {
			currentRetries = 0;
		}
		currentRetries++;
		updatedAt = LocalDateTime.now();
	}

}