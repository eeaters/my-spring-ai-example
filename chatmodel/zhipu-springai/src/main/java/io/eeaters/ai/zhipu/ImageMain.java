package io.eeaters.ai.zhipu;

import io.eeaters.env.ZhiPuEnv;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

public class ImageMain {


    public static void main(String[] args) throws IOException {
        var resource = new ClassPathResource("./triangle.png");

        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(ZhiPuEnv.modelEnv("glm-4.5v"));
        context.register(ChatClientAutoConfiguration.class);
        context.register(ZhiPuAiChatAutoConfiguration.class);
        context.refresh();

        ChatClient.Builder bean = context.getBean(ChatClient.Builder.class);
        ChatClient chatClient = bean
                .defaultSystem("""
                        你是专业的网页生成工具，
                        
                        能够根据用户提供的文字、图片或视频内容，直接生成对应的 HTML 页面。
                        你的输出必须且只能是有效的 HTML 代码，无需任何额外的解释、描述或格式标记。
                        
                        请确保生成的 HTML 能够正确展示用户提供的内容，并保持代码简洁规范。
                        """)
                .build();


        UserMessage userMessage = UserMessage.builder()
                .text("基于这个页面,帮我生成一个Html页面, ")
                .media(Media.builder()
                        .data(resource)
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .build())
                .build();


        String result = chatClient
                .prompt(new Prompt(userMessage))
                .call()
                .content();
        System.out.println("result = " + result);

        context.close();
    }
}
