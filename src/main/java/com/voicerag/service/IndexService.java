package com.voicerag.service;

import com.voicerag.config.VoiceRagConfig;
import com.voicerag.store.JsonFileEmbeddingStore;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class IndexService {
    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final VoiceRagConfig config;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    private boolean indexed = false;

    public IndexService(VoiceRagConfig config, EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        this.config = config;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        if (embeddingStore instanceof JsonFileEmbeddingStore jsonStore && jsonStore.size() > 0) {
            this.indexed = true;
        }
    }

    public String buildIndex() {
        Path transcriptsPath = Path.of(config.getTranscriptsDir());

        if (!Files.exists(transcriptsPath)) {
            return "Transcripts directory does not exist. Run transcription first.";
        }

        if (embeddingStore instanceof JsonFileEmbeddingStore jsonStore) {
            jsonStore.clear();
        }

        List<Path> txtFiles;
        try (Stream<Path> files = Files.list(transcriptsPath)) {
            txtFiles = files.filter(p -> p.toString().endsWith(".txt")).toList();
        } catch (IOException e) {
            return "Failed to list transcripts: " + e.getMessage();
        }

        if (txtFiles.isEmpty()) {
            return "No transcript files found. Run transcription first.";
        }

        int totalChunks = 0;

        for (Path txtFile : txtFiles) {
            try {
                String content = Files.readString(txtFile);
                List<String> chunks = chunkText(content);
                String source = txtFile.getFileName().toString();

                log.info("Processing {} ({} chunks)", source, chunks.size());

                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    TextSegment segment = TextSegment.from(chunk, Metadata.from("source", source).put("chunk_index", i));
                    Embedding embedding = embeddingModel.embed(segment).content();
                    embeddingStore.add(embedding, segment);
                    totalChunks++;
                }
            } catch (IOException e) {
                log.error("Failed to read {}: {}", txtFile, e.getMessage());
            }
        }

        indexed = true;
        return String.format("Indexed %d chunks from %d files", totalChunks, txtFiles.size());
    }

    public boolean isIndexed() {
        return indexed;
    }

    private List<String> chunkText(String text) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        int start = 0;
        while (start < words.length) {
            int end = Math.min(start + chunkSize, words.length);
            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                if (i > start) chunk.append(" ");
                chunk.append(words[i]);
            }
            String chunkStr = chunk.toString().trim();
            if (!chunkStr.isEmpty()) {
                chunks.add(chunkStr);
            }
            start = end - overlap;
            if (start >= words.length - overlap) break;
        }

        return chunks;
    }
}
