package io.eeaters.langchain.agentic.condition;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public interface CreativeWriter {


    @UserMessage("""
            你是一位创意写手。
            请围绕给定主题，生成一篇不超过三句话的故事草稿。
            仅返回故事，不返回其他任何内容。
            
            主题为 {{topic}}。
            """)
    @Agent(description = "基于topic生成一个故事", outputKey = "story")
    String generateStory(@V("topic") String topic);


}
