package io.eeaters.email.service;

import io.eeaters.email.client.EmailClient;
import io.eeaters.email.dto.EmailMessage;
import io.eeaters.email.dto.PullEmailRequest;
import io.eeaters.email.dto.SendEmailRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReplyService {

    private final EmailClient emailClient;

    private static final String NETEASE_IMAP_HOST = "imap.163.com";
    private static final Integer NETEASE_IMAP_PORT = 993;
    private static final String NETEASE_SMTP_HOST = "smtp.163.com";
    private static final Integer NETEASE_SMTP_PORT = 465;

    public void processEmailReplies() {
        try {
            String username = "todo@163.com";
            String password = "todo";
            String targetSubject = "回复：关于单号:MBL2025111009 的货物提货时间协调请求";

            PullEmailRequest pullRequest = PullEmailRequest.builder()
                    .host(NETEASE_IMAP_HOST)
                    .port(NETEASE_IMAP_PORT)
                    .username(username)
                    .password(password)
                    .protocol("imap")
                    .since(LocalDateTime.now().minusHours(24))
                    .maxCount(10)
                    .build();

            List<EmailMessage> emails = emailClient.pullEmail(pullRequest);

            emails = emails.stream().filter(message -> message.getMessageId().contains("<tencent_A8265324D0AADA19C6825353A020ACA4380A@qq.com>"))
                    .toList();
            log.info("Found {} emails matching criteria", emails.size());

            for (EmailMessage email : emails) {
                log.info("Processing email: {}", email.getSubject());
                replyToEmail(email, username, password);
            }

        } catch (Exception e) {
            log.error("Failed to process email replies: {}", e.getMessage(), e);
            throw new RuntimeException("邮件处理失败", e);
        }
    }

    private void replyToEmail(EmailMessage originalEmail, String username, String password) {
        try {
            String replyContent = buildReplyContent(originalEmail);

            String replySubject = buildReplySubject(originalEmail.getSubject());

            SendEmailRequest sendRequest = SendEmailRequest.builder()
                    .host(NETEASE_SMTP_HOST)
                    .port(NETEASE_SMTP_PORT)
                    .username(username)
                    .password(password)
                    .protocol("smtp")
                    .to(extractEmailFromAddress(originalEmail.getFrom()))
                    .subject(replySubject)
                    .content(replyContent)
                    .replyTo(originalEmail.getFrom())
                    .messageId(originalEmail.getMessageId())
                    .build();

            emailClient.sendEmail(sendRequest);
            log.info("Reply sent successfully for email: {}", originalEmail.getSubject());

        } catch (Exception e) {
            log.error("Failed to reply to email: {}", e.getMessage(), e);
            throw new RuntimeException("邮件回复失败", e);
        }
    }

    private String buildReplyContent(EmailMessage originalEmail) {
        StringBuilder content = new StringBuilder();

        content.append("很高兴收到你的回复,期待下次的合作\n\n");

        content.append("--------------------------------------------------------------------------------\n\n");

        content.append("------------------ 原始邮件 ------------------\n\n");

        content.append("发件人: ").append(originalEmail.getFrom()).append("\n");
        content.append("发送时间: ").append(originalEmail.getSentDate()).append("\n");
        content.append("收件人: ").append(originalEmail.getTo()).append("\n");
        if (originalEmail.getCc() != null) {
            content.append("抄送: ").append(originalEmail.getCc()).append("\n");
        } else {
            content.append("抄送: (无)\n");
        }
        content.append("主题: ").append(originalEmail.getSubject()).append("\n\n");

        if (originalEmail.getContent() != null) {
            content.append(originalEmail.getContent());
        }

        return content.toString();
    }

    private String buildReplySubject(String originalSubject) {
        if (originalSubject == null) {
            return "Re: ";
        }
        if (originalSubject.startsWith("Re: ") || originalSubject.startsWith("RE: ") ||
            originalSubject.startsWith("回复：") || originalSubject.startsWith("回复:")) {
            return originalSubject;
        }
        return "Re: " + originalSubject;
    }

    private String extractEmailFromAddress(String address) {
        if (address == null) {
            return null;
        }

        if (address.contains("<") && address.contains(">")) {
            int start = address.indexOf("<") + 1;
            int end = address.indexOf(">");
            return address.substring(start, end);
        }

        return address.trim();
    }
}