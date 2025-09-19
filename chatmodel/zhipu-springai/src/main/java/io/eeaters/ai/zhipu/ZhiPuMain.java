package io.eeaters.ai.zhipu;

import io.eeaters.Constants;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

public class ZhiPuMain {


    public static void main(String[] args) throws InterruptedException {
        var zhiPuAiApi = new ZhiPuAiApi(Constants.ZhiPu.apiKey);
        var chatModel = new ZhiPuAiChatModel(zhiPuAiApi);
        Flux<String> stream = chatModel.stream("好人不长命,祸害遗千年呀");
        stream.subscribe(res -> System.out.println("result  == " + res));

        TimeUnit.SECONDS.sleep(20);



    }

}
