package io.eeaters.statemachine.state;

/**
 * 邮件协调状态机事件定义
 *
 * 定义了触发状态转换的所有事件 事件是状态机状态转换的驱动力
 *
 * @author eeaters
 * @version 1.0.0
 */
public enum Events {

	/**
	 * 创建任务事件 - 触发从IDLE到TASK_CREATED的转换
	 */
	CREATE_TASK,

	/**
	 * 发送邮件事件 - 触发从TASK_CREATED到EMAIL_SENT的转换
	 */
	SEND_EMAIL,

	/**
	 * 开始轮询事件 - 触发从EMAIL_SENT到POLLING的转换
	 */
	START_POLLING,

	/**
	 * 接收响应事件 - 触发从POLLING到RESPONSE_RECEIVED的转换
	 */
	RECEIVE_RESPONSE,

	/**
	 * 发送第二封邮件事件 - 触发从RESPONSE_RECEIVED到SECOND_EMAIL_SENT的转换
	 */
	SEND_SECOND_EMAIL,

	/**
	 * 完成任务事件 - 触发任务完成的最终状态转换
	 */
	COMPLETE_TASK,

	/**
	 * 同意响应事件 - 表示收到同意的邮件回复
	 */
	RESPONSE_AGREE,

	/**
	 * 不同意响应事件 - 表示收到不同意的邮件回复
	 */
	RESPONSE_DISAGREE,

	/**
	 * 任务失败事件 - 触发任务失败的转换
	 */
	TASK_FAILED,

	/**
	 * 重置事件 - 重置状态机到初始状态
	 */
	RESET,

	/**
	 * 超时事件 - 任务执行超时
	 */
	TIMEOUT

}