package io.eeaters.single.function_call;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LifeKeyTools {


    @Tool(name = "getLifeChart", description = "根据用户的出生日期生成生命灵图和解读", returnDirect = true)
    public Response getLifeChart(Request request) {
        log.info("getLifeChart: {}", request);
        return new Response("/usr/local/img/life-demo.png", "你的生命力很顽强呀小子");
    }


    public record Request(
            @JsonPropertyDescription("用户的生日") String birthday
    ) {

    }


    public record Response(
            @JsonPropertyDescription("生命灵图的地址") String lifeUrl,
            @JsonPropertyDescription("生命灵图的解读") String description) {
    }

}
