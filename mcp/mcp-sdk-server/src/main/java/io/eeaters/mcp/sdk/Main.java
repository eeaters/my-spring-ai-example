package io.eeaters.mcp.sdk;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.eeaters.mcp.sdk.flux.Api;
import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import reactor.core.publisher.Mono;

import java.util.List;


@Configuration
public class Main {

   private static String emptyJsonSchema = """
			{
				"$schema": "http://json-schema.org/draft-07/schema#",
				"type": "object",
				"properties": {}
			}
			""";


    @Bean
    public WebFluxSseServerTransportProvider webFluxSseServerTransportProvider() {
        return Api.WebFluxSseServerTransportProvider();
    }


    public static void main(String[] args) throws LifecycleException {

        var tomcat = Api.tomcat();
        var tomContext = tomcat.addContext("", Api.baseDir);

        var applicationContext = new AnnotationConfigWebApplicationContext();
        applicationContext.register(Main.class);
        applicationContext.setServletContext(tomContext.getServletContext());
        applicationContext.refresh();

        DispatcherServlet dispatcherServlet = new DispatcherServlet(applicationContext);
        Wrapper wrapper = Tomcat.addServlet(tomContext, "dispatcherServlet", dispatcherServlet);
        wrapper.setLoadOnStartup(1);
        tomContext.addServletMappingDecoded("/*", "dispatcherServlet");


        var bean = applicationContext.getBean(WebFluxSseServerTransportProvider.class);

        McpSchema.Tool too = new McpSchema.Tool("empty-Tools", "Duplicate tool", emptyJsonSchema);

        McpAsyncServer mcpAsyncServer = McpServer.async(bean)
                .serverInfo("test", "1.0.0")
                .objectMapper(new ObjectMapper())
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .toolCall(too, (exchange, arg) -> Mono.just(new McpSchema.CallToolResult(List.of(), false)))
                .build();


        tomcat.start();


    }




}
