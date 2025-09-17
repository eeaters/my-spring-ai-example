package io.eeaters.event;

import io.eeaters.Constants;
import io.eeaters.single.function_call.LifeKeyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;


public class EventHandlerMain {


    private static final String SYSTEM_PROMPT = """
            角色：运维分析师
            任务：基于提供的实时告警内容，分析系统/应用异常根本原因，提供诊断步骤与修复方案，并支持自动化执行。
            """;


    public static void main(String[] args) {
        System.out.println("是否执行处理方案. 请输入 Y 或者 N");
        String input = System.console().readLine();
        if(!"Y".equalsIgnoreCase(input)) {
            System.out.println("任务已终止");
        }

        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of(
                "spring.ai.openai.apiKey", io.eeaters.Constants.DeepSeek.apiKey,
                "spring.ai.openai.chat.baseUrl", io.eeaters.Constants.DeepSeek.baseUrl,
                "spring.ai.openai.chat.options.model", Constants.DeepSeek.modelName
        ));
        environment.getPropertySources().addFirst(propertySource);

        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(TestConfig.class);
        context.register(ChatClientAutoConfiguration.class);
        context.register(ToolCallingAutoConfiguration.class);
        context.register(OpenAiChatAutoConfiguration.class);
        context.refresh();


        ChatClient.Builder clientBuilder = context.getBean(ChatClient.Builder.class);
        Tools tools = context.getBean(Tools.class);

        String content = clientBuilder.defaultSystem(SYSTEM_PROMPT)
                .defaultTools(tools)
                .build()
                .prompt("""
                        告警：数据库连接数超过设定阈值
                        时间：2024-08-03 15:30:00
                        """)
                .call()
                .content();
        System.out.println(content);


    }



    @Configuration
    public static class TestConfig{

        @Bean
        public Tools tools() {
            return new Tools();
        }
    }

}
