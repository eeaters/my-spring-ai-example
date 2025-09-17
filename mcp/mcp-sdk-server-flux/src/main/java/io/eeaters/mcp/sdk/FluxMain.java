package io.eeaters.mcp.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Mono;

import java.util.List;

@SpringBootApplication
public class FluxMain {

    public static void main(String[] args) {
        SpringApplication.run(FluxMain.class, args);
    }

    private static final String emptyJsonSchema = """
            {
                "$schema": "http://json-schema.org/draft-07/schema#",
                "type": "object",
                "properties": {}
            }
            """;

    @Bean
    public WebFluxSseServerTransportProvider getTransportProvider() {
        return WebFluxSseServerTransportProvider.builder()
                .messageEndpoint("sse/message")
                .objectMapper(new ObjectMapper())
                .build();
    }


    @Bean
    public McpAsyncServer getAsyncServer(WebFluxSseServerTransportProvider transportProvider) {
        McpSchema.Tool tool = new McpSchema.Tool(
                "empty tools",
                "this is an empty tool for test",
                emptyJsonSchema
        );

        return McpServer.async(transportProvider)
                .serverInfo("test", "1.0.0")
                .objectMapper(new ObjectMapper())
                .toolCall(tool, (exchange, arg) ->
                        Mono.just(new McpSchema.CallToolResult.Builder()
                                .content(List.of())
                                .build())
                ).build();

    }

    @Bean
    public RouterFunction<?> routerFunction(WebFluxSseServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }


}
