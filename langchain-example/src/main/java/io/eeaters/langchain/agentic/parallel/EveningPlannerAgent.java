package io.eeaters.langchain.agentic.parallel;


import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.V;

import java.util.List;

/**
 *
 *
 * @author YuJie Wan
 * @since 0.1
 */
public interface EveningPlannerAgent {


    @Agent
    List<EveningPlan> plan(@V("mood") String mood);



    record EveningPlan(String movie, String meal) { }

}
