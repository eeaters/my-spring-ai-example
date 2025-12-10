package io.eeaters.langchain.agentic.condition;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.scope.AgenticScopeAccess;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.service.V;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public interface StyleWriter extends AgenticScopeAccess {

    @Agent
    ResultWithAgenticScope<String> writeStoryWithStyle(@V("topic") String topic, @V("style") String style);
}