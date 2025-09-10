package io.eeaters.ai.zhipu.multi_fun.function_call;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class LifeKeyTools {


    @Tool(name = "getLifeChart", description = "根据用户的出生日期生成生命灵图和解读")
    public GeneratePicResponse analysisLifeKey(GeneratePicRequest generatePicRequest) {
        log.info("analysisLifeKey: {}", generatePicRequest);
        return new GeneratePicResponse("https://www.baidu.com", "你好, 再见");
    }


    public record GeneratePicRequest(@JsonPropertyDescription("用户的生日，格式 YYYY-MM-DD") LocalDate birthday) {

    }

    public record GeneratePicResponse(
            @JsonPropertyDescription("图片存放的路径") String url,
            @JsonPropertyDescription("生命密码介绍") String lifeDetail
    ) {
    }







}
