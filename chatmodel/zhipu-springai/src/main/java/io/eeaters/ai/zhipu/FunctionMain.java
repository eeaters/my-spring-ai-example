package io.eeaters.ai.zhipu;


import io.eeaters.ai.zhipu.function_call.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

public class FunctionMain {

    public static void main(String[] args) {

        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of("spring.ai.zhipuai.apiKey", "api-key"));
        environment.getPropertySources().addFirst(propertySource);

        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(WeatherTools.class);
        context.register(ChatClientAutoConfiguration.class);
        context.register(ToolCallingAutoConfiguration.class);
        context.register(ZhiPuAiChatAutoConfiguration.class);
        context.refresh();

        ChatClient.Builder bean = context.getBean(ChatClient.Builder.class);
        WeatherTools weatherTools = context.getBean(WeatherTools.class);
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
