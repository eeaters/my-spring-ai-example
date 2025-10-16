package io.eeaters;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.videos.VideoCreateParams;
import ai.z.openapi.service.videos.VideoResult;
import ai.z.openapi.service.videos.VideosResponse;

import java.util.List;

public class VideoMain {

    public static void main(String[] args) {
        // 初始化客户端
        ZhipuAiClient client = ZhipuAiClient.builder()
                .apiKey(Constants.ZhiPu.apiKey)
                .build();

//        var params = VideoCreateParams.builder()
//                .model("cogvideox-3")
//                .duration(10)
//                .prompt("""
//                        ## 主题:
//                           何方道友在此渡劫
//
//                        ## 简述
//                           东海上空雷云翻滚,形成一条雷龙, 好像要将整个天地吞噬, 下面一人手持一剑正渡劫,仙风飘飘
//                        """)
//
//                .build();
//        VideosResponse videosResponse = client.videos().videoGenerations(params);
//        System.out.println("videosResponse.getData() = " + videosResponse.getData());
//        System.out.println("videosResponse.getData().getId() = " + videosResponse.getData().getId());
//        System.out.println("videosResponse.getData().getRequestId() = " + videosResponse.getData().getRequestId());

        //   202510161121274cad21167f7a47d4

        VideosResponse videosResponse = client.videos().videoGenerationsResult("43181752647475473-8253018079582209822");
        System.out.println("videosResponse.getData().getTaskStatus() = " + videosResponse.getData().getTaskStatus());
        List<VideoResult> videoResult = videosResponse.getData().getVideoResult();
        System.out.println("videosResponse.getData().getOptimizedPrompt() = " + videosResponse.getData().getOptimizedPrompt());
        for (VideoResult result : videoResult) {
            System.out.println("result = " + result);
            System.out.println(result.getCoverImageUrl());
        }


    }

}
