package io.eeaters.providers;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class ToolProvider {


    @McpTool(name = "upper-case", description = "Convert input to upper case")
    public String upperCase(String input) {
        return input.toUpperCase();
    }

}
