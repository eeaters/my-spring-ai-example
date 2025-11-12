package io.eeaters.email.client;

import io.eeaters.email.dto.EmailMessage;
import io.eeaters.email.dto.PullEmailRequest;
import io.eeaters.email.dto.SendEmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SubjectTerm;
import jakarta.mail.search.ReceivedDateTerm;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
public class NetEaseEmailClient implements EmailClient {

    /**
     * 连接到IMAP服务器，专门为网易邮箱优化
     */
    private Store connectToImap(PullEmailRequest request) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.imap.host", request.getHost());
        props.put("mail.imap.port", String.valueOf(request.getPort()));
        props.put("mail.imap.auth", "true");
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.imap.socketFactory.fallback", "false");

        Session session = Session.getInstance(props);
        session.setDebug(false);

        Store store = session.getStore("imap");
        store.connect(request.getUsername(), request.getPassword());

        // 为网易邮箱设置IMAP ID信息
        String host = request.getHost().toLowerCase();
        if (host.contains("163.com") || host.contains("126.com") || host.contains("yeah.net")) {
            try {
                // 使用反射调用IMAPStore的id方法，避免直接依赖具体实现
                if (store.getClass().getName().contains("IMAPStore")) {
                    Map<String, String> imapId = new HashMap<>();
                    imapId.put("name", "email-reply-assistant");
                    imapId.put("version", "1.0.0");
                    imapId.put("vendor", "eeaters");
                    imapId.put("support-email", "support@eeaters.io");

                    // 使用反射调用id方法
                    var idMethod = store.getClass().getMethod("id", Map.class);
                    idMethod.invoke(store, imapId);
                    log.info("为网易邮箱设置IMAP ID成功: {}", imapId);
                }
            } catch (Exception e) {
                log.warn("设置网易邮箱IMAP ID失败，但连接可能仍然有效: {}", e.getMessage());
            }
        }

        return store;
    }

    @Override
    public List<EmailMessage> pullEmail(PullEmailRequest request) throws Exception {
        Store store = connectToImap(request);

        try {

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            List<SearchTerm> searchTerms = new ArrayList<>();


            if (request.getSince() != null) {
                Date sinceDate = Date.from(request.getSince().atZone(ZoneId.systemDefault()).toInstant());
                searchTerms.add(new ReceivedDateTerm(ReceivedDateTerm.GE, sinceDate));
            }

            Message[] messages;
            if (searchTerms.isEmpty()) {
                messages = inbox.getMessages();
            } else {
                SearchTerm combinedTerm = new AndTerm(searchTerms.toArray(new SearchTerm[0]));
                messages = inbox.search(combinedTerm);
            }

            int maxCount = request.getMaxCount() != null ? request.getMaxCount() : Integer.MAX_VALUE;

            List<EmailMessage> result = Arrays.stream(messages)
                    .sorted((m1, m2) -> {
                        try {
                            Date d1 = m1.getReceivedDate();
                            Date d2 = m2.getReceivedDate();
                            return d2.compareTo(d1);
                        } catch (MessagingException e) {
                            return 0;
                        }
                    })
                    .limit(maxCount)
                    .map(this::convertToEmailMessage)
                    .filter(Objects::nonNull)
                    .toList();

            inbox.close(false);
            store.close();

            return result;

        } catch (Exception e) {
            log.error("Failed to pull emails: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void sendEmail(SendEmailRequest request) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", request.getHost());
        props.put("mail.smtp.port", String.valueOf(request.getPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(request.getUsername(), request.getPassword());
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(request.getUsername()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.getTo()));

            if (request.getCc() != null && !request.getCc().trim().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(request.getCc()));
            }

            message.setSubject(request.getSubject(), "UTF-8");
            message.setText(request.getContent(), "UTF-8");

            if (request.getReplyTo() != null) {
                message.setReplyTo(InternetAddress.parse(request.getReplyTo()));
            }

            if (request.getMessageId() != null) {
                message.setHeader("In-Reply-To", request.getMessageId());
                message.setHeader("References", request.getMessageId());
            }

            Transport.send(message);
            log.info("Email sent successfully to: {}", request.getTo());

        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
            throw e;
        }
    }

    private EmailMessage convertToEmailMessage(Message message) {
        try {
            String content = getTextContent(message);

            return EmailMessage.builder()
                    .messageId(getHeaderValue(message, "Message-ID"))
                    .from(message.getFrom() != null && message.getFrom().length > 0 ? message.getFrom()[0].toString() : null)
                    .to(getRecipientsAsString(message, Message.RecipientType.TO))
                    .cc(getRecipientsAsString(message, Message.RecipientType.CC))
                    .subject(message.getSubject())
                    .content(content)
                    .receivedDate(convertToLocalDateTime(message.getReceivedDate()))
                    .sentDate(convertToLocalDateTime(message.getSentDate()))
                    .replyTo(getReplyToAsString(message))
                    .inReplyTo(getHeaderValue(message, "In-Reply-To"))
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert message: {}", e.getMessage(), e);
            return null;
        }
    }

    private String getTextContent(Message message) throws Exception {
        if (message.isMimeType("text/plain")) {
            return (String) message.getContent();
        } else if (message.isMimeType("text/html")) {
            return (String) message.getContent();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart multipart = (MimeMultipart) message.getContent();
            return getTextFromMultipart(multipart);
        }
        return "";
    }

    private String getTextFromMultipart(MimeMultipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        int count = multipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("multipart/*")) {
                result.append(getTextFromMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private String getHeaderValue(Message message, String headerName) {
        try {
            String[] headers = message.getHeader(headerName);
            return headers != null && headers.length > 0 ? headers[0] : null;
        } catch (MessagingException e) {
            return null;
        }
    }

    private String getRecipientsAsString(Message message, Message.RecipientType type) {
        try {
            Address[] recipients = message.getRecipients(type);
            if (recipients != null && recipients.length > 0) {
                return Arrays.stream(recipients)
                        .map(Address::toString)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(null);
            }
        } catch (MessagingException e) {
            log.debug("Failed to get recipients: {}", e.getMessage());
        }
        return null;
    }

    private String getReplyToAsString(Message message) {
        try {
            Address[] replyTo = message.getReplyTo();
            if (replyTo != null && replyTo.length > 0) {
                return replyTo[0].toString();
            }
        } catch (MessagingException e) {
            log.debug("Failed to get reply-to: {}", e.getMessage());
        }
        return null;
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}