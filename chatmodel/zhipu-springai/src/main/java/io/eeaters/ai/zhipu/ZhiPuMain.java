package io.eeaters.ai.zhipu;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class ZhiPuMain {


    public static void main(String[] args) throws InterruptedException {
        var zhiPuAiApi = new ZhiPuAiApi("0fca8048ed5b4ee8a8c3e2b82bd1c78a.b43le2hcL0jpb0gO");
        var chatModel = new ZhiPuAiChatModel(zhiPuAiApi);
        Flux<String> stream = chatModel.stream("好人不长命,祸害遗千年呀");
        stream.subscribe(res -> System.out.println("result  == " + res));

        TimeUnit.SECONDS.sleep(20);

        ChatClient.Builder builder = ChatClient.builder(chatModel);


    }

}
