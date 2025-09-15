package io.eeaters.mcp.sdk.flux;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.transport.WebFluxSseServerTransportProvider;
import org.apache.catalina.startup.Tomcat;

public class Api {

    public static String baseDir = System.getProperty("java.io.tmpdir");


    public static WebFluxSseServerTransportProvider WebFluxSseServerTransportProvider() {
        return WebFluxSseServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .messageEndpoint("mcp/message")
                .build();
    }


    public static Tomcat tomcat() {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8888);
        tomcat.setBaseDir(baseDir);
        return tomcat;
    }



}
