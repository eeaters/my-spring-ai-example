package io.eeaters.ai.zhipu.multi_fun;


import io.eeaters.Constants;
import io.eeaters.ai.zhipu.multi_fun.function_call.LifeKeyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

public class MultiFunMain {

    public static void main(String[] args) {

        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of("spring.ai.zhipuai.apiKey", Constants.ZhiPu.apiKey));
        environment.getPropertySources().addFirst(propertySource);

        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(LifeKeyTools.class);
        context.register(ChatClientAutoConfiguration.class);
        context.register(ToolCallingAutoConfiguration.class);
        context.register(ZhiPuAiChatAutoConfiguration.class);
        context.refresh();

        ChatClient.Builder bean = context.getBean(ChatClient.Builder.class);
        LifeKeyTools lifeKeyTools = context.getBean(LifeKeyTools.class);
        ChatClient chatClient = bean
                .defaultSystem("""
                        你是一个专业的生命密码（Life Key）专家，角色设定如下：
                        
                        1. 你的身份：
                           - 你是一位温暖、积极、专业的生命密码专家。
                           - 你擅长通过生日推算用户的生命灵图，并给出有建设性、积极向上的解读。
                           - 在非专业话题对话中，你会用正向能量引导用户，保持亲切和鼓励的语气。

                        2. 关于工具调用（Function Call）：
                           - 如果用户明确提供了完整的出生日期（如 "1990-05-21" 或 "2001年7月3日"），你不要直接给出生命灵图的结果，而是触发调用 `getLifeChart` 工具。
                           - `getLifeChart` 工具会返回一个生命灵图的图片地址以及详细解读。
                           - 工具调用完成后，你要基于工具结果，给用户一个简短温暖的解读，并鼓励他们从中获取积极力量。

                        3. 对话引导：
                           - 如果用户没有提供生日信息，但在聊生活、情绪或人生问题，你应以积极向上的态度回应，帮助他们看到希望与价值。
                           - 如果用户闲聊，你要保持轻松愉快的对话氛围，传递乐观能量。

                        请始终保持：
                        - 温暖、专业、积极、鼓励的语气。
                        - 回答简洁易懂，避免使用生硬的术语。
                        
                        """)
                .defaultTools(lifeKeyTools)
                .build();

        var result = chatClient
                .prompt("""
                         你好, 我的生日是 1992年 8月16日
                        """)
                .call()
                .entity(LifeKeyTools.GeneratePicResponse.class);
        System.out.println("result = " + result);

        context.close();
    }
}
