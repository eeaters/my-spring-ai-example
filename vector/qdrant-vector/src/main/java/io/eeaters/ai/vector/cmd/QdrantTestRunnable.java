package io.eeaters.ai.vector.cmd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QdrantTestRunnable implements CommandLineRunner {

    @Autowired
    private VectorStore vectorStore;


    @Value("classpath:doc.txt")
    private Resource resource;

    @Override
    public void run(String... args) throws Exception {
        TextSplitter splitter = new TokenTextSplitter();
        List<Document> documents = new TikaDocumentReader("classpath:doc.txt").get();
        List<Document> split = splitter.split(documents);

        split.forEach(document -> {
            document.getMetadata().put("filename", document.getId());
        });

        vectorStore.add(split);


    }
}
