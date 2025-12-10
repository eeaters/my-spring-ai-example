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
public interface StyleEditor {

    @UserMessage("""
            您是一位专业编辑。
            
            请分析并改写以下故事，使其更符合 {{style}} 风格，并使其更加连贯。
            仅返回故事内容，无需其他任何内容。
            
            故事内容为“{{story}}”。
            """)
    @Agent(value = "修改故事以更好地符合特定风格", outputKey = "story")
    String editStory(@V("story") String story, @V("style") String style);

}
