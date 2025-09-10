package io.eeaters;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.embedding.EmbeddingCreateParams;
import ai.z.openapi.service.embedding.EmbeddingResponse;

import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class EmbeddingMain
{
    public static void main(String[] args) {
        // 初始化客户端
        ZhipuAiClient client = ZhipuAiClient.builder()
                .apiKey("api-key")
                .build();

        // 创建向量化请求
        EmbeddingCreateParams request = EmbeddingCreateParams.builder()
                .model("embedding-2")
                .input(Arrays.asList("Hello world", "How are you?", "How is the weather today?"))
                .build();

        // 发送请求
        EmbeddingResponse response = client.embeddings().createEmbeddings(request);
        System.out.println("向量: " + response.getData());
    }
}
