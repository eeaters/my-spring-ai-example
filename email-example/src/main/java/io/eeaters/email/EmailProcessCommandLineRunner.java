package io.eeaters.email;

import io.eeaters.email.service.EmailReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailProcessCommandLineRunner implements CommandLineRunner {

    private final EmailReplyService emailReplyService;

    @Override
    public void run(String... args) throws Exception {
        log.info("应用启动完成，开始自动处理邮件回复...");

        try {
            emailReplyService.processEmailReplies();
            log.info("邮件回复处理完成");
        } catch (Exception e) {
            log.error("邮件回复处理失败: {}", e.getMessage(), e);
        }

        log.info("邮件处理任务结束，应用继续运行");
    }
}