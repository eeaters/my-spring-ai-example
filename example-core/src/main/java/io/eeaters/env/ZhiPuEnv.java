package io.eeaters.env;

import io.eeaters.Constants;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

public interface ZhiPuEnv {

    static ConfigurableEnvironment env() {
        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of("spring.ai.zhipuai.apiKey", Constants.ZhiPu.apiKey));
        environment.getPropertySources().addFirst(propertySource);
        return environment;
    }

    static ConfigurableEnvironment modelEnv(String modelName) {
        var environment = new StandardEnvironment();
        var propertySource = new MapPropertySource("demo", Map.of(
                "spring.ai.zhipuai.apiKey", Constants.ZhiPu.apiKey,
                "spring.ai.zhipuai.chat.options.model", modelName
        ));
        environment.getPropertySources().addFirst(propertySource);
        return environment;
    }
}
