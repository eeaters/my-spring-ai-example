package io.eeaters;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.File;

public class FileParseMain {

    // {"message":"任务创建成功","task_id":"8210537044b34656b0dc7d12edee1f63"}
    public static void main(String[] args) {

//        upload();
        
        // 查询任务结果
//        query("8210537044b34656b0dc7d12edee1f63",  "download_link");
    }

    private static void upload() {
        try {

            var file = new ClassPathResource("133 MA.pdf");

            RestClient restClient = RestClient.builder()
                .baseUrl("https://open.bigmodel.cn/api")
                .build();

            // 创建文件资源
            FileSystemResource fileResource = new FileSystemResource(file.getFile());

            // 创建multipart表单数据
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("tool_type", "Prime");
            body.add("file_type", "PDF");
            body.add("file", fileResource);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(Constants.ZhiPu.apiKey);

            // 创建请求实体
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 发送POST请求
            String response = restClient.post()
                    .uri("/paas/v4/files/parser/create")
                    .headers(httpHeaders -> {
                        httpHeaders.addAll(headers);
                    })
                    .body(body)
                    .retrieve()
                    .body(String.class);

            System.out.println("响应结果: " + response);

        } catch (Exception e) {
            System.err.println("请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 查询文件解析任务结果
     * @param taskId 任务ID
     * @param formatType 返回格式类型 (json, text, markdown)
     */
    private static void query(String taskId, String formatType) {
        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl("https://open.bigmodel.cn/api")
                    .build();

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(Constants.ZhiPu.apiKey);

            // 发送GET请求
            String response = restClient.get()
                    .uri("/paas/v4/files/parser/result/{taskId}/{formatType}", taskId, formatType)
                    .headers(httpHeaders -> {
                        httpHeaders.addAll(headers);
                    })
                    .retrieve()
                    .body(String.class);

            System.out.println("任务查询结果: " + response);

        } catch (Exception e) {
            System.err.println("查询失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
