package io.eeaters.langchain.agentic.condition;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface StyleScore {

    @UserMessage("""
            您是一位严格的审稿人。
            
            请根据以下故事与风格“{{style}}”的契合程度，给出 0.0 到 1.0 之间的评分。
            仅返回评分，不返回其他任何内容。
            
            故事如下："{{story}}"
            """)
    @Agent(description = "根据故事与特定风格的契合程度进行评分", outputKey = "score")
    double scoreStyle(@V("story") String story, @V("style") String style);

}
