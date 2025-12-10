package io.eeaters.langchain.agentic.parallel;

import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.eeaters.langchain.agentic.ModelUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public class ParallelAgents {


    public static void main(String[] args) {

        ChatModel chatModel = ModelUtils.getChatModel();

        FoodExpert foodExpert = AgenticServices.agentBuilder(FoodExpert.class)
                .chatModel(chatModel)
                .outputKey("meals")
                .build();
        MovieExpert movieExpert = AgenticServices.agentBuilder(MovieExpert.class)
                .chatModel(chatModel)
                .outputKey("movies")
                .build();

        EveningPlannerAgent agent = AgenticServices.parallelBuilder(EveningPlannerAgent.class)
                .subAgents(foodExpert, movieExpert)
                .outputKey("plans")
                .output(scope -> {
                    List<String> movies = scope.readState("movies", List.of());
                    List<String> meals = scope.readState("meals", List.of());
                    List<EveningPlannerAgent.EveningPlan> moviesAndMeals = new ArrayList<>();
                    for (int i = 0; i < movies.size(); i++) {
                        if (i >= meals.size()) {
                            break;
                        }
                        moviesAndMeals.add(new EveningPlannerAgent.EveningPlan(movies.get(i), meals.get(i)));
                    }
                    return moviesAndMeals;
                }).executor(Executors.newFixedThreadPool(2))
                .build();

        var romantic = agent.plan("romantic");
        for (EveningPlannerAgent.EveningPlan eveningPlan : romantic) {
            System.out.println(eveningPlan);
        }

    }

}
