package io.eeaters.email.controller;

import io.eeaters.email.service.EmailReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailReplyService emailReplyService;

    @PostMapping("/process-replies")
    public String processReplies() {
        try {
            emailReplyService.processEmailReplies();
            return "邮件处理完成";
        } catch (Exception e) {
            log.error("邮件处理失败: {}", e.getMessage(), e);
            return "邮件处理失败: " + e.getMessage();
        }
    }
}