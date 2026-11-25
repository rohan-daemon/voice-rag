package com.voicerag.config;

import com.voicerag.store.JsonFileEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class EmbeddingConfig {

    @Value("${voicerag.transcripts-dir}")
    private String transcriptsDir;

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        Path embeddingsFile = Path.of(transcriptsDir).resolve("embeddings.json");
        return new JsonFileEmbeddingStore(embeddingsFile);
    }
}
