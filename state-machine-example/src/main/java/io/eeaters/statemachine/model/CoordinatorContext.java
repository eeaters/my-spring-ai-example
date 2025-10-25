package io.eeaters.statemachine.model;

import io.eeaters.statemachine.state.States;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 协调器上下文模型
 *
 * 存储状态机执行过程中的所有上下文数据 作为状态机的Extended State，在不同状态之间传递数据
 *
 * @author eeaters
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatorContext {

	/**
	 * 当前任务ID
	 */
	private String currentTaskId;

	/**
	 * 当前邮件任务
	 */
	private EmailTask currentTask;

	/**
	 * 当前邮件响应
	 */
	private EmailResponse currentResponse;

	/**
	 * 当前状态机状态
	 */
	private States currentState;

	/**
	 * 状态机实例ID
	 */
	private String stateMachineId;

	/**
	 * 流程开始时间
	 */
	private LocalDateTime startTime;

	/**
	 * 最后更新时间
	 */
	private LocalDateTime lastUpdateTime;

	/**
	 * 轮询计数器 - 记录轮询次数
	 */
	private Integer pollCount;

	/**
	 * 已发送邮件数量
	 */
	private Integer sentEmailCount;

	/**
	 * 错误信息 - 存储执行过程中的错误
	 */
	private String errorMessage;

	/**
	 * 上次错误时间
	 */
	private LocalDateTime lastErrorTime;

	/**
	 * 扩展属性 - 存储额外的业务数据
	 */
	private Map<String, Object> extendedAttributes;

	/**
	 * 决策变量 - 存储状态机Guard和Choice需要的决策数据
	 */
	private Map<String, Object> decisionVariables;

	/**
	 * 临时数据 - 存储状态转换过程中的临时数据
	 */
	private Map<String, Object> temporaryData;

	public CoordinatorContext(String taskId) {
		this.currentTaskId = taskId;
		this.startTime = LocalDateTime.now();
		this.lastUpdateTime = LocalDateTime.now();
		this.pollCount = 0;
		this.sentEmailCount = 0;
		this.extendedAttributes = new ConcurrentHashMap<>();
		this.decisionVariables = new ConcurrentHashMap<>();
		this.temporaryData = new ConcurrentHashMap<>();
	}

	/**
	 * 添加扩展属性
	 * @param key 属性键
	 * @param value 属性值
	 */
	public void addExtendedAttribute(String key, Object value) {
		if (extendedAttributes == null) {
			extendedAttributes = new ConcurrentHashMap<>();
		}
		extendedAttributes.put(key, value);
		updateLastUpdateTime();
	}

	/**
	 * 设置扩展属性
	 * @param key 属性键
	 * @param value 属性值
	 */
	public void setExtendedAttribute(String key, Object value) {
		if (extendedAttributes == null) {
			extendedAttributes = new ConcurrentHashMap<>();
		}
		extendedAttributes.put(key, value);
		updateLastUpdateTime();
	}

	/**
	 * 获取扩展属性
	 * @param key 属性键
	 * @param <T> 属性值类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	public <T> T getExtendedAttribute(String key) {
		if (extendedAttributes == null) {
			return null;
		}
		return (T) extendedAttributes.get(key);
	}

	/**
	 * 设置决策变量
	 * @param key 变量键
	 * @param value 变量值
	 */
	public void setDecisionVariable(String key, Object value) {
		if (decisionVariables == null) {
			decisionVariables = new ConcurrentHashMap<>();
		}
		decisionVariables.put(key, value);
		updateLastUpdateTime();
	}

	/**
	 * 获取决策变量
	 * @param key 变量键
	 * @param <T> 变量值类型
	 * @return 变量值
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDecisionVariable(String key) {
		if (decisionVariables == null) {
			return null;
		}
		return (T) decisionVariables.get(key);
	}

	/**
	 * 设置临时数据
	 * @param key 数据键
	 * @param value 数据值
	 */
	public void setTemporaryData(String key, Object value) {
		if (temporaryData == null) {
			temporaryData = new ConcurrentHashMap<>();
		}
		temporaryData.put(key, value);
		updateLastUpdateTime();
	}

	/**
	 * 获取临时数据
	 * @param key 数据键
	 * @param <T> 数据值类型
	 * @return 数据值
	 */
	@SuppressWarnings("unchecked")
	public <T> T getTemporaryData(String key) {
		if (temporaryData == null) {
			return null;
		}
		return (T) temporaryData.get(key);
	}

	/**
	 * 增加轮询计数
	 */
	public void incrementPollCount() {
		if (pollCount == null) {
			pollCount = 0;
		}
		pollCount++;
		updateLastUpdateTime();
	}

	/**
	 * 增加发送邮件计数
	 */
	public void incrementSentEmailCount() {
		if (sentEmailCount == null) {
			sentEmailCount = 0;
		}
		sentEmailCount++;
		updateLastUpdateTime();
	}

	/**
	 * 设置错误信息
	 * @param errorMessage 错误信息
	 */
	public void setError(String errorMessage) {
		this.errorMessage = errorMessage;
		this.lastErrorTime = LocalDateTime.now();
		updateLastUpdateTime();
	}

	/**
	 * 清除错误信息
	 */
	public void clearError() {
		this.errorMessage = null;
		this.lastErrorTime = null;
		updateLastUpdateTime();
	}

	/**
	 * 检查是否有错误
	 * @return true如果有错误信息
	 */
	public boolean hasError() {
		return errorMessage != null && !errorMessage.trim().isEmpty();
	}

	/**
	 * 更新最后更新时间
	 */
	private void updateLastUpdateTime() {
		this.lastUpdateTime = LocalDateTime.now();
	}

	/**
	 * 获取流程执行时长（秒）
	 * @return 执行时长
	 */
	public long getExecutionDurationSeconds() {
		if (startTime == null) {
			return 0;
		}
		return java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds();
	}

	/**
	 * 清理临时数据
	 */
	public void clearTemporaryData() {
		if (temporaryData != null) {
			temporaryData.clear();
		}
		updateLastUpdateTime();
	}

}