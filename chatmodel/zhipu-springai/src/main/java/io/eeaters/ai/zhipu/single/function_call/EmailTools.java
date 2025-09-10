package io.eeaters.ai.zhipu.single.function_call;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailTools {


    @Tool(name = "sendEmail", description = "发送邮件到指定邮箱中")
    public void sendEmail(Request request) {
        log.info("sendEmail: {}", request);
    }


    public record Request(
            @JsonPropertyDescription("邮箱") String email,
            @JsonPropertyDescription("邮件内容") String content
    ) {

    }


}
