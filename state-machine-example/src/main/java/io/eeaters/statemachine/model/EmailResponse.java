package io.eeaters.statemachine.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 邮件响应模型
 *
 * 表示从目标邮箱收到的邮件回复信息 包含邮件内容、发送时间、响应类型等关键信息
 *
 * @author eeaters
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

	/**
	 * 响应ID - 唯一标识一个邮件响应
	 */
	private String responseId;

	/**
	 * 关联的任务ID
	 */
	private String taskId;

	/**
	 * 邮件主题
	 */
	private String subject;

	/**
	 * 邮件内容
	 */
	private String content;

	/**
	 * 邮件发送方
	 */
	private String fromEmail;

	/**
	 * 邮件接收方
	 */
	private String toEmail;

	/**
	 * 邮件接收时间
	 */
	private LocalDateTime receivedAt;

	/**
	 * 响应类型 - 分析得出的响应结果
	 */
	private ResponseType responseType;

	/**
	 * 置信度 - AI分析的置信度分数 (0.0-1.0)
	 */
	private Double confidence;

	/**
	 * 分析备注 - AI分析的详细说明
	 */
	private String analysisNote;

	/**
	 * 是否已处理
	 */
	private Boolean processed;

	/**
	 * 响应类型枚举
	 */
	public enum ResponseType {

		AGREE, // 同意
		DISAGREE, // 不同意
		UNCLEAR, // 不明确
		NEED_INFO, // 需要更多信息
		NOT_RELATED // 不相关

	}

	/**
	 * 邮件类型枚举
	 */
	public enum EmailType {

		INITIAL, // 初始邮件
		FOLLOW_UP // 跟进邮件

	}

	/**
	 * 判断是否为积极响应（同意）
	 * @return true如果响应类型为同意
	 */
	public boolean isPositiveResponse() {
		return ResponseType.AGREE.equals(responseType);
	}

	/**
	 * 判断是否为消极响应（不同意）
	 * @return true如果响应类型为不同意
	 */
	public boolean isNegativeResponse() {
		return ResponseType.DISAGREE.equals(responseType);
	}

	/**
	 * 判断响应是否明确
	 * @return true如果响应类型不是不明确或不相关
	 */
	public boolean isClearResponse() {
		return responseType != null && !ResponseType.UNCLEAR.equals(responseType)
				&& !ResponseType.NOT_RELATED.equals(responseType);
	}

	/**
	 * 判断置信度是否足够高
	 * @param threshold 置信度阈值
	 * @return true如果置信度大于等于阈值
	 */
	public boolean hasHighConfidence(double threshold) {
		return confidence != null && confidence >= threshold;
	}

	/**
	 * 标记为已处理
	 */
	public void markAsProcessed() {
		this.processed = true;
	}

}