package io.eeaters.cmd;

import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.util.List;

/**
 * @author yjwan
 * @version 1.0
 */
//@Component
public class McpCommandLineRunnable implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    List<McpSyncClient> syncClients;

    @Autowired
    ToolCallbackProvider tools;

    @Override
    public void run(String... args) throws Exception {
        McpSyncClient mcpSyncClient = syncClients.get(0);
        String content = chatClientBuilder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .defaultToolCallbacks(tools)
                .build()
                .prompt("今天的天气如何")
                .call()
                .content();
        System.out.println("content = " + content);
    }
}

    