package io.eeaters.statemachine.agent;

import io.eeaters.statemachine.config.EmailConfig;
import io.eeaters.statemachine.model.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 响应分析代理
 *
 * 使用关键词分析邮件响应内容 判断响应类型（同意/不同意/不明确等）并给出置信度
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResponseAnalyzerAgent {

	private final EmailConfig emailConfig;

	/**
	 * 分析邮件响应
	 * @param taskId 任务ID
	 * @param subject 邮件主题
	 * @param content 邮件内容
	 * @param fromEmail 发件人邮箱
	 * @param toEmail 收件人邮箱
	 * @return 分析结果
	 */
	public EmailResponse analyzeResponse(String taskId, String subject, String content, String fromEmail,
			String toEmail) {
		log.info("开始分析邮件响应: taskId={}, from={}, subject={}", taskId, fromEmail, subject);

		try {
			EmailResponse.ResponseType responseType;
			double confidence;
			String analysisNote;

			// 首先尝试关键词快速分析
			KeywordAnalysisResult keywordResult = analyzeByKeywords(content);

			if (keywordResult.isConfident()) {
				// 关键词分析置信度高，直接使用结果
				responseType = keywordResult.getResponseType();
				confidence = keywordResult.getConfidence();
				analysisNote = "基于关键词分析: " + keywordResult.getMatchedKeywords();
			}
			else {
				// 使用规则进行进一步分析
				RuleAnalysisResult ruleResult = analyzeByRules(content);
				responseType = ruleResult.getResponseType();
				confidence = ruleResult.getConfidence();
				analysisNote = ruleResult.getAnalysisNote();
			}

			EmailResponse response = EmailResponse.builder()
				.responseId(UUID.randomUUID().toString())
				.taskId(taskId)
				.subject(subject)
				.content(content)
				.fromEmail(fromEmail)
				.toEmail(toEmail)
				.receivedAt(java.time.LocalDateTime.now())
				.responseType(responseType)
				.confidence(confidence)
				.analysisNote(analysisNote)
				.processed(false)
				.build();

			log.info("邮件响应分析完成: taskId={}, responseType={}, confidence={}", response.getTaskId(),
					response.getResponseType(), response.getConfidence());

			return response;

		}
		catch (Exception e) {
			log.error("邮件响应分析失败: taskId={}, error={}", taskId, e.getMessage(), e);

			// 返回默认的不明确响应
			return EmailResponse.builder()
				.responseId(UUID.randomUUID().toString())
				.taskId(taskId)
				.subject(subject)
				.content(content)
				.fromEmail(fromEmail)
				.toEmail(toEmail)
				.receivedAt(java.time.LocalDateTime.now())
				.responseType(EmailResponse.ResponseType.UNCLEAR)
				.confidence(0.0)
				.analysisNote("分析失败: " + e.getMessage())
				.processed(false)
				.build();
		}
	}

	/**
	 * 基于关键词的快速分析
	 */
	private KeywordAnalysisResult analyzeByKeywords(String content) {
		String lowerContent = content.toLowerCase();

		// 检查同意关键词
		Set<String> agreeKeywords = new HashSet<>(Arrays.asList("同意", "确认", "好的", "ok", "yes", "accept", "确认参加"));
		agreeKeywords
			.addAll(emailConfig.getResponseAnalysis().getAgreeKeywords().stream().map(String::toLowerCase).toList());

		// 检查不同意关键词
		Set<String> disagreeKeywords = new HashSet<>(
				Arrays.asList("不同意", "拒绝", "不能", "no", "reject", "无法参加", "不行", "不可以"));
		disagreeKeywords
			.addAll(emailConfig.getResponseAnalysis().getDisagreeKeywords().stream().map(String::toLowerCase).toList());

		// 统计匹配的关键词
		List<String> matchedAgreeKeywords = agreeKeywords.stream().filter(lowerContent::contains).toList();

		List<String> matchedDisagreeKeywords = disagreeKeywords.stream().filter(lowerContent::contains).toList();

		// 判断响应类型
		if (!matchedAgreeKeywords.isEmpty() && matchedDisagreeKeywords.isEmpty()) {
			return new KeywordAnalysisResult(EmailResponse.ResponseType.AGREE, 0.8, "匹配同意关键词: " + matchedAgreeKeywords,
					true);
		}
		else if (!matchedDisagreeKeywords.isEmpty() && matchedAgreeKeywords.isEmpty()) {
			return new KeywordAnalysisResult(EmailResponse.ResponseType.DISAGREE, 0.8,
					"匹配不同意关键词: " + matchedDisagreeKeywords, true);
		}
		else if (!matchedAgreeKeywords.isEmpty() && !matchedDisagreeKeywords.isEmpty()) {
			return new KeywordAnalysisResult(EmailResponse.ResponseType.UNCLEAR, 0.3,
					"同时匹配多种关键词: " + matchedAgreeKeywords + ", " + matchedDisagreeKeywords, false);
		}
		else {
			return new KeywordAnalysisResult(EmailResponse.ResponseType.UNCLEAR, 0.1, "未匹配任何关键词", false);
		}
	}

	/**
	 * 基于规则的进一步分析
	 */
	private RuleAnalysisResult analyzeByRules(String content) {
		String lowerContent = content.toLowerCase();

		// 规则1: 明确的积极表达
		if (lowerContent.contains("我同意") || lowerContent.contains("确认参加") || lowerContent.contains("很高兴")
				|| lowerContent.contains("期待")) {
			return new RuleAnalysisResult(EmailResponse.ResponseType.AGREE, 0.9, "明确积极表达");
		}

		// 规则2: 明确的消极表达
		if (lowerContent.contains("不能参加") || lowerContent.contains("时间冲突") || lowerContent.contains("抱歉")
				|| lowerContent.contains("无法")) {
			return new RuleAnalysisResult(EmailResponse.ResponseType.DISAGREE, 0.9, "明确消极表达");
		}

		// 规则3: 需要更多信息
		if (lowerContent.contains("请问") || lowerContent.contains("什么时间") || lowerContent.contains("具体安排")
				|| lowerContent.contains("详细信息")) {
			return new RuleAnalysisResult(EmailResponse.ResponseType.NEED_INFO, 0.6, "询问更多信息");
		}

		// 规则4: 短回复，可能不明确
		if (content.trim().length() < 10) {
			return new RuleAnalysisResult(EmailResponse.ResponseType.UNCLEAR, 0.2, "回复过短，可能不明确");
		}

		// 默认为不明确
		return new RuleAnalysisResult(EmailResponse.ResponseType.UNCLEAR, 0.4, "内容不够明确");
	}

	/**
	 * 关键词分析结果
	 */
	private static class KeywordAnalysisResult {

		private final EmailResponse.ResponseType responseType;

		private final double confidence;

		private final String matchedKeywords;

		private final boolean isConfident;

		public KeywordAnalysisResult(EmailResponse.ResponseType responseType, double confidence, String matchedKeywords,
				boolean isConfident) {
			this.responseType = responseType;
			this.confidence = confidence;
			this.matchedKeywords = matchedKeywords;
			this.isConfident = isConfident;
		}

		public EmailResponse.ResponseType getResponseType() {
			return responseType;
		}

		public double getConfidence() {
			return confidence;
		}

		public String getMatchedKeywords() {
			return matchedKeywords;
		}

		public boolean isConfident() {
			return isConfident;
		}

	}

	/**
	 * 规则分析结果
	 */
	private static class RuleAnalysisResult {

		private final EmailResponse.ResponseType responseType;

		private final double confidence;

		private final String analysisNote;

		public RuleAnalysisResult(EmailResponse.ResponseType responseType, double confidence, String analysisNote) {
			this.responseType = responseType;
			this.confidence = confidence;
			this.analysisNote = analysisNote;
		}

		public EmailResponse.ResponseType getResponseType() {
			return responseType;
		}

		public double getConfidence() {
			return confidence;
		}

		public String getAnalysisNote() {
			return analysisNote;
		}

	}

}