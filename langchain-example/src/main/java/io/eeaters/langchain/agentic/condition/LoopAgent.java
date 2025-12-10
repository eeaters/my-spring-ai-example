package io.eeaters.langchain.agentic.condition;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.ResultWithAgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import io.eeaters.langchain.agentic.ModelUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public class LoopAgent {

    public static void main(String[] args) {
        AtomicInteger loopCounter = new AtomicInteger(0);

        ChatModel chatModel = ModelUtils.getChatModel();

        CreativeWriter writer = AgenticServices.agentBuilder(CreativeWriter.class)
                .chatModel(chatModel)
                .afterAgentInvocation(agentResponse -> {
                    System.out.println("agentResponse.agenticScope() = " + agentResponse.agenticScope());
                })
                .outputKey("story")
                .build();

        StyleEditor editor =  AgenticServices.agentBuilder(StyleEditor.class)
                .chatModel(chatModel)
                .afterAgentInvocation(agentResponse -> {
                    System.out.println("agentResponse.agenticScope() = " + agentResponse.agenticScope());
                })
                .outputKey("story")
                .build();
        StyleScore styleScorer = AgenticServices.agentBuilder(StyleScore.class)
                .chatModel(chatModel)
                .afterAgentInvocation(agentResponse -> {
                    System.out.println("agentResponse.agenticScope() = " + agentResponse.agenticScope());
                })
                .outputKey("score")
                .build();

        UntypedAgent loopAgent = AgenticServices.loopBuilder()
                .subAgents(editor, styleScorer)
                .maxIterations(5)
                .testExitAtLoopEnd(true)
                .exitCondition((agenticScope, loop) -> {
                    loopCounter.set(loop);
                    return agenticScope.readState("score", 0.0) >= 0.8;
                }).build();


        StyleWriter styleWriter = AgenticServices.sequenceBuilder(StyleWriter.class)
                .subAgents(writer, loopAgent)
                .outputKey("story")
                .build();

        ResultWithAgenticScope<String> stringResultWithAgenticScope = styleWriter.writeStoryWithStyle("dragons and wizards", "comdy");

        System.out.println("stringResultWithAgenticScope.result() = " + stringResultWithAgenticScope.result());
    }


}
