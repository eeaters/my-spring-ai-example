package io.eeaters.function_call;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yjwan
 * @version 1.0
 */
@Component
public class FunctionCallCommandLineRunnable implements CommandLineRunner {

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @Autowired
    List<McpSyncClient> syncClients;

    @Autowired
    ToolCallbackProvider tools;

    @Override
    public void run(String... args) throws Exception {
        McpSyncClient mcpSyncClient = syncClients.get(0);
        McpSchema.ListResourcesResult listResourcesResult = mcpSyncClient.listResources();
        McpSchema.ListPromptsResult listPromptsResult = mcpSyncClient.listPrompts();
        McpSchema.ListToolsResult listToolsResult = mcpSyncClient.listTools();

        McpSchema.GetPromptResult prompt = mcpSyncClient.getPrompt(new McpSchema.GetPromptRequest("conversation-start", null));
        System.out.println("prompt = " + prompt);
    }
}

    