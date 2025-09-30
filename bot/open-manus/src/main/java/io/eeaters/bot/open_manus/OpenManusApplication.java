package io.eeaters.bot.open_manus;

import io.eeaters.Constants;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OpenManusApplication {

    public static void main(String[] args) {
        System.setProperty("spring.ai.zhipuai.apiKey", Constants.ZhiPu.apiKey);
        System.setProperty("spring.ai.zhipuai.chat.options.model", ZhiPuAiApi.ChatModel.GLM_4_5.getValue());
        SpringApplication.run(OpenManusApplication.class, args);
    }
}
