package io.eeaters.statemachine.service;

import io.eeaters.statemachine.config.EmailConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Message;
import jakarta.mail.Folder;
import jakarta.mail.Store;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.ComparisonTerm;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * 邮件服务类
 *
 * 负责发送邮件和接收邮件的核心功能 支持SMTP发送和IMAP/POP3接收
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender mailSender;

	private final EmailConfig emailConfig;

	@Value("${spring.mail.username}")
	private String fromEmail;

	@Value("${spring.mail.password}")
	private String emailPassword;

	@Value("${spring.mail.host}")
	private String mailHost;

	/**
	 * 发送简单文本邮件
	 * @param to 收件人邮箱
	 * @param subject 邮件主题
	 * @param content 邮件内容
	 * @return true如果发送成功，false否则
	 */
	public boolean sendSimpleEmail(String to, String subject, String content) {
		try {
			log.info("开始发送邮件到: {}, 主题: {}", to, subject);

			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(content);

			mailSender.send(message);

			log.info("邮件发送成功: to={}, subject={}", to, subject);
			return true;

		}
		catch (Exception e) {
			log.error("邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 发送HTML格式邮件
	 * @param to 收件人邮箱
	 * @param subject 邮件主题
	 * @param content HTML邮件内容
	 * @return true如果发送成功，false否则
	 */
	public boolean sendHtmlEmail(String to, String subject, String content) {
		try {
			log.info("开始发送HTML邮件到: {}, 主题: {}", to, subject);

			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setFrom(fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(content, true);

			mailSender.send(message);

			log.info("HTML邮件发送成功: to={}, subject={}", to, subject);
			return true;

		}
		catch (Exception e) {
			log.error("HTML邮件发送失败: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 检查邮箱新邮件
	 * @param startTime 检查起始时间
	 * @return 新邮件列表
	 */
	public List<EmailMessage> checkNewEmails(LocalDateTime startTime) {
		List<EmailMessage> newEmails = new ArrayList<>();

		try {
			log.info("开始检查邮箱新邮件，起始时间: {}", startTime);

			Store store = getMailStore();
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);

			// 搜索指定时间后的邮件
			Date searchDate = Date.from(startTime.atZone(ZoneId.systemDefault()).toInstant());
			ReceivedDateTerm term = new ReceivedDateTerm(ComparisonTerm.GT, searchDate);
			Message[] messages = inbox.search(term);

			log.info("找到 {} 封新邮件", messages.length);

			for (Message message : messages) {
				try {
					EmailMessage emailMessage = parseMessage(message);
					newEmails.add(emailMessage);
				}
				catch (Exception e) {
					log.warn("解析邮件失败: {}", e.getMessage());
				}
			}

			inbox.close(false);
			store.close();

			log.info("邮箱检查完成，获取到 {} 封新邮件", newEmails.size());
			return newEmails;

		}
		catch (Exception e) {
			log.error("检查邮箱失败: {}", e.getMessage(), e);
			return newEmails;
		}
	}

	/**
	 * 获取邮件存储连接
	 */
	private Store getMailStore() throws Exception {
		// 这里使用IMAP协议，实际使用时需要根据邮件服务器配置
		java.util.Properties props = new java.util.Properties();
		props.put("mail.store.protocol", "imaps");
		props.put("mail.imaps.host", mailHost);
		props.put("mail.imaps.port", "993");
		props.put("mail.imaps.ssl.enable", "true");

		jakarta.mail.Session session = jakarta.mail.Session.getInstance(props);
		Store store = session.getStore("imaps");
		store.connect(fromEmail, emailPassword);

		return store;
	}

	/**
	 * 解析邮件消息
	 */
	private EmailMessage parseMessage(Message message) throws Exception {
		EmailMessage emailMessage = new EmailMessage();

		emailMessage.setFrom(Arrays.toString(message.getFrom()));
		emailMessage.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
		emailMessage.setSubject(message.getSubject());
		emailMessage
			.setReceivedDate(message.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());

		// 获取邮件内容
		if (message.isMimeType("text/plain")) {
			emailMessage.setContent((String) message.getContent());
		}
		else if (message.isMimeType("multipart/*")) {
			emailMessage.setContent(extractTextFromMultipart(message));
		}
		else {
			emailMessage.setContent(message.getContent().toString());
		}

		return emailMessage;
	}

	/**
	 * 从多部分邮件中提取文本内容
	 */
	private String extractTextFromMultipart(Message message) throws Exception {
		// 简化实现，实际项目中需要更复杂的解析逻辑
		Object content = message.getContent();
		if (content instanceof String) {
			return (String) content;
		}
		return message.getContent().toString();
	}

	/**
	 * 邮件消息模型
	 */
	public static class EmailMessage {

		private String from;

		private String to;

		private String subject;

		private String content;

		private LocalDateTime receivedDate;

		// Getters and Setters
		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getTo() {
			return to;
		}

		public void setTo(String to) {
			this.to = to;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public LocalDateTime getReceivedDate() {
			return receivedDate;
		}

		public void setReceivedDate(LocalDateTime receivedDate) {
			this.receivedDate = receivedDate;
		}

	}

}