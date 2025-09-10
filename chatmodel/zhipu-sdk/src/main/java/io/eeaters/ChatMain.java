package io.eeaters;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatThinking;

import java.util.ArrayList;
import java.util.List;

public class ChatMain {

    static List<String> modelList = List.of(
            "glm-z1-air",
            "glm-z1-airx",
            "glm-z1-flashx",

            "glm-4.5",
            "glm-4.5v",
            "glm-4.5-air",
            "glm-4.5-x",
            "glm-4.5-airx",
            "GLM-4.5-FLASH",


            "glm-4-flash",
            "glm-4v-flash",
            "glm-z1-flash",
            "glm-4.1v-thinking-flash",


            "GLM-4",
            "glm-4v",
            "glm-4-air",
            "glm-4-airx",
            "glm-4-flash",
            "GLM-3-Turbo"
    );







    public static void main(String[] args) {
        List<String> successList = new ArrayList<>();
        List<String> errorList = new ArrayList<>();

        // 初始化客户端
        ZhipuAiClient client = ZhipuAiClient.builder()
                .apiKey(Constants.ZhiPu.apiKey)
                .build();
        for (String s : modelList) {
            try{
                var params = ChatCompletionCreateParams.builder()
                        .model(s)
                        .messages(List.of(ChatMessage.builder()
                                .role("user")
                                .content("ping , and you can response me pong!")
                                .build()
                        ))
                        .thinking(ChatThinking.builder().type("DISABLED".toLowerCase()).build())
                        .build();

                ChatCompletionResponse chatCompletion = client.chat().createChatCompletion(params);
                log(s, chatCompletion);
                successList.add(s);
            }catch (Exception e){
                System.out.println( s + " = " + e.getMessage());
                errorList.add(s);
            }
        }
        String collect = String.join(",", successList);
        System.out.println("[success]: " + collect);

        String collect1 = String.join(",", errorList);
        System.out.println("[error]: " +collect1);

    }


    private static void log(String model,ChatCompletionResponse chatCompletion) {
        try {
            System.out.println(model + " = " + chatCompletion.getData().getChoices().getFirst().getMessage().getContent());
        } catch (Exception e) {
        }
    }


}
