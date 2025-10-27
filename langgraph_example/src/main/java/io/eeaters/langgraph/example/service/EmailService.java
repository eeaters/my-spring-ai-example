package io.eeaters.langgraph.example.service;

import io.eeaters.langgraph.example.config.EmailConfig;
import io.eeaters.langgraph.example.model.EmailResponse;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailService {

	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

	@Autowired
	private EmailConfig emailConfig;

	@Autowired
	private JavaMailSender mailSender;

	private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2})[:：](\\d{2})\\s*(AM|PM|上午|下午)?",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-/年](\\d{1,2})[-/月](\\d{1,2})[日]?");

	public String sendEmail(String to, String subject, String content) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(emailConfig.getFromAddress());
			message.setTo(to);
			message.setSubject(subject);
			message.setText(content);

			mailSender.send(message);

			String messageId = UUID.randomUUID().toString();
			logger.info("Email sent successfully to: {}, subject: {}, messageId: {}", to, subject, messageId);

			return messageId;
		}
		catch (Exception e) {
			logger.error("Failed to send email to: {}", to, e);
			throw new RuntimeException("Failed to send email", e);
		}
	}

	public List<EmailResponse> checkNewEmails(String lastEmailThreadId) {
		List<EmailResponse> responses = new ArrayList<>();

		Properties props = new Properties();
		props.put("mail.store.protocol", emailConfig.getProtocol());
		props.put("mail.imap.ssl.enable", emailConfig.isSsl());
		props.put("mail.imap.host", emailConfig.getHost());
		props.put("mail.imap.port", emailConfig.getPort());

		Session session = Session.getDefaultInstance(props);
		session.setDebug(emailConfig.isEnableDebug());

		try {
			Store store = session.getStore(emailConfig.getProtocol());
			store.connect(emailConfig.getUsername(), emailConfig.getPassword());

			Folder inbox = store.getFolder(emailConfig.getInboxFolder());
			inbox.open(Folder.READ_ONLY);

			Message[] messages = inbox.getMessages();
			for (Message message : messages) {
				if (isMessageNewerThan(message, lastEmailThreadId)) {
					EmailResponse response = parseMessage(message);
					if (response != null) {
						responses.add(response);
					}
				}
			}

			inbox.close(false);
			store.close();

		}
		catch (Exception e) {
			logger.error("Failed to check emails", e);
			throw new RuntimeException("Failed to check emails", e);
		}

		return responses;
	}

	private boolean isMessageNewerThan(Message message, String lastThreadId) throws MessagingException {
		if (lastThreadId == null) {
			return true;
		}

		String[] references = message.getHeader("References");
		if (references != null && references.length > 0) {
			for (String ref : references) {
				if (ref.contains(lastThreadId)) {
					return true;
				}
			}
		}

		String subject = message.getSubject();
		return subject != null && subject.toLowerCase().contains("re:");
	}

	private EmailResponse parseMessage(Message message) {
		try {
			String id = ((MimeMessage) message).getMessageID();
			String from = Arrays.toString(message.getFrom());
			String to = Arrays.toString(message.getRecipients(Message.RecipientType.TO));
			String subject = message.getSubject();

			Object content = message.getContent();
			String contentStr = content instanceof String ? (String) content : content.toString();

			LocalDateTime receivedAt = LocalDateTime.now();
			String emailThreadId = extractThreadId(message);

			boolean isPositive = analyzeSentiment(contentStr);
			LocalDateTime proposedTime = extractProposedTime(contentStr);
			String sender = extractSenderName(message);

			return EmailResponse.builder()
				.id(id)
				.from(from)
				.to(to)
				.subject(subject)
				.content(contentStr)
				.receivedAt(receivedAt)
				.emailThreadId(emailThreadId)
				.isPositive(isPositive)
				.proposedTime(proposedTime)
				.sender(sender)
				.build();

		}
		catch (Exception e) {
			logger.error("Failed to parse message", e);
			return null;
		}
	}

	private String extractThreadId(Message message) throws MessagingException {
		String[] references = message.getHeader("References");
		if (references != null && references.length > 0) {
			return references[0];
		}

		String[] inReplyTo = message.getHeader("In-Reply-To");
		if (inReplyTo != null && inReplyTo.length > 0) {
			return inReplyTo[0];
		}

		return ((MimeMessage) message).getMessageID();
	}

	private String extractSenderName(Message message) throws MessagingException {
		Address[] from = message.getFrom();
		if (from != null && from.length > 0) {
			if (from[0] instanceof InternetAddress) {
				InternetAddress address = (InternetAddress) from[0];
				return address.getPersonal() != null ? address.getPersonal() : address.getAddress();
			}
			return from[0].toString();
		}
		return "Unknown";
	}

	private boolean analyzeSentiment(String content) {
		if (content == null) {
			return false;
		}

		String lowerContent = content.toLowerCase();

		List<String> positiveKeywords = Arrays.asList("同意", "可以", "好的", "ok", "没问题", "确认", "安排", "yes", "approve");

		List<String> negativeKeywords = Arrays.asList("不行", "不可以", "有问题", "困难", "无法", "不能", "no", "reject", "不同意");

		for (String keyword : negativeKeywords) {
			if (lowerContent.contains(keyword)) {
				return false;
			}
		}

		for (String keyword : positiveKeywords) {
			if (lowerContent.contains(keyword)) {
				return true;
			}
		}

		return false;
	}

	private LocalDateTime extractProposedTime(String content) {
		if (content == null) {
			return null;
		}

		Matcher dateMatcher = DATE_PATTERN.matcher(content);
		Matcher timeMatcher = TIME_PATTERN.matcher(content);

		int year = LocalDateTime.now().getYear();
		int month = LocalDateTime.now().getMonthValue();
		int day = LocalDateTime.now().getDayOfMonth();
		int hour = 10;
		int minute = 0;

		if (dateMatcher.find()) {
			year = Integer.parseInt(dateMatcher.group(1));
			month = Integer.parseInt(dateMatcher.group(2));
			day = Integer.parseInt(dateMatcher.group(3));
		}

		if (timeMatcher.find()) {
			hour = Integer.parseInt(timeMatcher.group(1));
			minute = Integer.parseInt(timeMatcher.group(2));

			String period = timeMatcher.group(3);
			if (period != null) {
				period = period.toLowerCase();
				if ((period.contains("pm") || period.contains("下午")) && hour < 12) {
					hour += 12;
				}
				else if ((period.contains("am") || period.contains("上午")) && hour == 12) {
					hour = 0;
				}
			}
		}

		try {
			return LocalDateTime.of(year, month, day, hour, minute);
		}
		catch (Exception e) {
			logger.warn("Failed to parse proposed time from content: {}", content, e);
			return null;
		}
	}

}