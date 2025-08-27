package io.eeaters.cmd;

import io.eeaters.function_call.UpperTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author yjwan
 * @version 1.0
 */
@Component
public class LocalCommandLineRunnable implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Override
    public void run(String... args) throws Exception {
        Method method = ReflectionUtils.findMethod(UpperTools.class, "upperTools", String.class);
        var tool = MethodToolCallback.builder()
                .toolDefinition(ToolDefinition.builder()
                        .name("upperTools")
                        .description("uppercase your input")
                        .inputSchema("""
                                {
                                  "type": "object",
                                  "properties": {
                                    "input": {
                                      "type": "string",
                                      "description": "The text to be converted to uppercase"
                                    }
                                  },
                                  "required": ["input"]
                                }
                                """)
                        .build())
                .toolMethod(method)
                .toolObject(new UpperTools())
                .build();

        String content = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultSystem("you can use tools to uppercase user's input")
                .defaultToolCallbacks(tool)
                .build()
                .prompt("hello hello hello ,hello every one")
                .call()
                .content();
        System.out.println("content = " + content);
    }
}

