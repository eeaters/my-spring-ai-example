package io.eeaters.single;


import io.eeaters.Constants;
import io.eeaters.single.function_call.EmailTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

public class FunctionMain {

    public static void main(String[] args) {

        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of(
                "spring.ai.openai.apiKey", Constants.DeepSeek.apiKey,
                "spring.ai.openai.chat.baseUrl", Constants.DeepSeek.baseUrl,
                "spring.ai.openai.chat.options.model", Constants.DeepSeek.modelName
        ));

        environment.getPropertySources().addFirst(propertySource);

        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(EmailTools.class);
        context.register(ChatClientAutoConfiguration.class);
        context.register(ToolCallingAutoConfiguration.class);
        context.register(OpenAiChatAutoConfiguration.class);
        context.refresh();

        ChatClient.Builder bean = context.getBean(ChatClient.Builder.class);
        EmailTools weatherTools = context.getBean(EmailTools.class);
        ChatClient chatClient = bean
                .defaultTools(weatherTools)
                .build();

        String result = chatClient
                .prompt("""
                        给我往 83748475@qq.com 这个邮箱里发一个邮件; 邮件的内容为: 
                        
                       `你好, 你的验证码为: 9352`
                        """)
                .call().content();
        System.out.println("result = " + result);

        context.close();
    }
}
