package io.eeaters.statemachine.state;

/**
 * 邮件协调状态机状态定义
 *
 * 定义了邮件协调过程中的所有可能状态 使用枚举确保类型安全和状态管理的一致性
 *
 * @author eeaters
 * @version 1.0.0
 */
public enum States {

	/**
	 * 空闲状态 - 系统初始状态或任务完成后的状态
	 */
	IDLE,

	/**
	 * 任务已创建 - 邮件协调任务已经初始化
	 */
	TASK_CREATED,

	/**
	 * 邮件已发送 - 初始邮件已经发送到目标邮箱
	 */
	EMAIL_SENT,

	/**
	 * 轮询中 - 正在检查邮箱等待回复
	 */
	POLLING,

	/**
	 * 收到响应 - 已经收到邮件回复
	 */
	RESPONSE_RECEIVED,

	/**
	 * 第二封邮件已发送 - 发送跟进邮件后的状态
	 */
	SECOND_EMAIL_SENT,

	/**
	 * 任务完成 - 收到同意回复，任务成功结束
	 */
	COMPLETED,

	/**
	 * 任务失败 - 发生错误或超时，任务异常结束
	 */
	FAILED

}