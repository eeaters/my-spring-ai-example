package io.eeaters.statemachine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 邮件协调器配置属性
 *
 * 从application.yml中读取邮件协调相关的配置 提供类型安全的配置访问方式
 *
 * @author eeaters
 * @version 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "email-coordinator")
public class EmailConfig {

	/**
	 * 轮询配置
	 */
	private PollingConfig polling = new PollingConfig();

	/**
	 * 邮件模板配置
	 */
	private TemplateConfig templates = new TemplateConfig();

	/**
	 * 响应分析配置
	 */
	private ResponseAnalysisConfig responseAnalysis = new ResponseAnalysisConfig();

	/**
	 * 任务配置
	 */
	private TaskConfig task = new TaskConfig();

	@Data
	public static class PollingConfig {

		/**
		 * 是否启用轮询
		 */
		private boolean enabled = true;

		/**
		 * 轮询间隔（毫秒）
		 */
		private long interval = 30000;

		/**
		 * 最大轮询次数
		 */
		private int maxAttempts = 100;

		/**
		 * 初始延迟（毫秒）
		 */
		private long initialDelay = 5000;

	}

	@Data
	public static class TemplateConfig {

		/**
		 * 初始邮件模板
		 */
		private EmailTemplate initialEmail = new EmailTemplate();

		/**
		 * 跟进邮件模板
		 */
		private EmailTemplate followUpEmail = new EmailTemplate();

		@Data
		public static class EmailTemplate {

			private String subject;

			private String body;

		}

	}

	@Data
	public static class ResponseAnalysisConfig {

		/**
		 * 同意关键词列表
		 */
		private List<String> agreeKeywords = List.of("同意", "确认", "好的", "OK", "yes", "accept", "确认参加");

		/**
		 * 不同意关键词列表
		 */
		private List<String> disagreeKeywords = List.of("不同意", "拒绝", "不能", "no", "reject", "无法参加");

		/**
		 * AI分析置信度阈值
		 */
		private double confidenceThreshold = 0.7;

		/**
		 * 是否启用AI分析
		 */
		private boolean enableAiAnalysis = true;

	}

	@Data
	public static class TaskConfig {

		/**
		 * 任务超时时间（毫秒）
		 */
		private long timeout = 3600000; // 1小时

		/**
		 * 失败重试次数
		 */
		private int retryCount = 3;

		/**
		 * 最大并发任务数
		 */
		private int maxConcurrentTasks = 10;

		/**
		 * 任务清理间隔（毫秒）
		 */
		private long cleanupInterval = 600000; // 10分钟

	}

}