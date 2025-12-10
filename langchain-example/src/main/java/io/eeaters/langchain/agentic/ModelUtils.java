package io.eeaters.langchain.agentic;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public class ModelUtils {

    public static ChatModel getChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com")
                .modelName("deepseek-chat")
                .apiKey("")
                .build();
    }
}
