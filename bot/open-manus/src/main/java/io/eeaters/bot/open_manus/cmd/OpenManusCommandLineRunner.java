package io.eeaters.bot.open_manus.cmd;

import io.eeaters.bot.open_manus.agent.ManusAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class OpenManusCommandLineRunner implements CommandLineRunner {


    @Autowired
    ManusAgent manusAgent;

    @Override
    public void run(String... args) throws Exception {
//        String s = manusAgent.run("/Users/yujie/IdeaProjects/my-spring-ai-example/bot/open-manus/src/main/java/io/eeaters/bot/open_manus/agent/ManusAgent.java 这个类实现的是什么功能")
//                .get();

        CompletableFuture<String> run = manusAgent.run("我电脑的python版本是多少; ");
        System.out.println("---------" + run.get());
    }
}
