package io.eeaters.providers;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptProvider {

    @McpPrompt(name = "greeting", description = "A simple greeting prompt")
    public McpSchema.GetPromptResult greetingPrompt(
            @McpArg(name = "name", description = "The name to greet", required = true) String name) {
        return new McpSchema.GetPromptResult("Greeting", List.of(new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
                new McpSchema.TextContent("Hello, " + name + "! Welcome to the MCP system."))));
    }


    @McpPrompt(name = "conversation-start", description = "A simple conversation start prompt")
    public List<McpSchema.PromptMessage> conversationStart(McpSchema.GetPromptRequest request) {
        return List.of(
                new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(
                        """
                                Hello, I'm the MCP assitant , How can I help you today
                                """
                )),
                new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(
                        """
                                I'd like to learn more about the Model Context Protocol
                                """
                )),
                new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT, new McpSchema.TextContent(
                        """
                                MCP is an open protocol that standardizes how applications provide context to large language models (LLMs).
                                Think of MCP like a USB-C port for AI applications.
                                Just as USB-C provides a standardized way to connect your devices to various peripherals and accessories,
                                MCP provides a standardized way to connect AI models to different data sources and tools.
                                MCP enables you to build agents and complex workflows on top of LLMs and connects your models with the world.
                                """
                ))
        );
    }
}
