package io.eeaters.langchain.agentic.parallel;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;

import java.util.List;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public interface FoodExpert {

    @UserMessage("""
            You are a great evening planner.
            Propose a list of 3 meals matching the given mood.
            The mood is {{mood}}.
            For each meal, just give the name of the meal.
            Provide a list with the 3 items and nothing else.
            """)
    @Agent(outputKey = "meals")
    List<String> findMeal(String mood);


}
