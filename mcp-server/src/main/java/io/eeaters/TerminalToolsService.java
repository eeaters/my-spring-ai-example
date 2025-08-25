package io.eeaters;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * @author yjwan
 * @version 1.0
 */
@Component
public class TerminalToolsService {


    private static final Logger log = LoggerFactory.getLogger(TerminalToolsService.class);

    @Tool(description = "send Email", returnDirect = true)
    public void send(Request request) {
        log.info("Sending email");
    }


    public record Request(@JsonProperty("email") String email) {

    }
}

    