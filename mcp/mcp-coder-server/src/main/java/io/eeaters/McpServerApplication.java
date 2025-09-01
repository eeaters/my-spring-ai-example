package io.eeaters;

import org.springaicommunity.mcp.provider.complete.SyncMcpCompleteProvider;
import org.springaicommunity.mcp.provider.prompt.SyncMcpPromptProvider;
import org.springaicommunity.mcp.provider.resource.SyncMcpResourceProvider;
import org.springaicommunity.mcp.provider.tool.SyncMcpToolProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 *
 */
@SpringBootApplication
public class McpServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }


}
