package com.voicerag.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JsonFileEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final Logger log = LoggerFactory.getLogger(JsonFileEmbeddingStore.class);

    private final Path filePath;
    private final ObjectMapper objectMapper;
    private final List<EmbeddingEntry> entries;

    public JsonFileEmbeddingStore(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper();
        this.entries = new CopyOnWriteArrayList<>(loadFromFile());
    }

    @Override
    public String add(Embedding embedding) {
        String id = UUID.randomUUID().toString();
        entries.add(new EmbeddingEntry(id, null, embedding.vectorAsList(), Map.of()));
        saveToFile();
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        entries.add(new EmbeddingEntry(id, null, embedding.vectorAsList(), Map.of()));
        saveToFile();
    }

    @Override
    public String add(Embedding embedding, TextSegment textSegment) {
        String id = UUID.randomUUID().toString();
        Map<String, String> metadata = new HashMap<>();
        if (textSegment.metadata() != null) {
            textSegment.metadata().asMap().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
        }
        entries.add(new EmbeddingEntry(id, textSegment.text(), embedding.vectorAsList(), metadata));
        saveToFile();
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            String id = UUID.randomUUID().toString();
            entries.add(new EmbeddingEntry(id, null, embedding.vectorAsList(), Map.of()));
            ids.add(id);
        }
        saveToFile();
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> textSegments) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            String id = UUID.randomUUID().toString();
            TextSegment segment = textSegments.get(i);
            Map<String, String> metadata = new HashMap<>();
            if (segment.metadata() != null) {
                segment.metadata().asMap().forEach((k, v) -> metadata.put(k, String.valueOf(v)));
            }
            entries.add(new EmbeddingEntry(id, segment.text(), embeddings.get(i).vectorAsList(), metadata));
            ids.add(id);
        }
        saveToFile();
        return ids;
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults) {
        return findRelevant(referenceEmbedding, maxResults, 0.0);
    }

    @Override
    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (EmbeddingEntry entry : entries) {
            float[] entryVector = toFloatArray(entry.getEmbedding());
            double score = cosineSimilarity(referenceEmbedding.vector(), entryVector);
            if (score >= minScore) {
                Metadata metadata = new Metadata(new HashMap<>(entry.getMetadata()));
                TextSegment segment = entry.getText() != null
                        ? TextSegment.from(entry.getText(), metadata)
                        : null;
                Embedding embedding = Embedding.from(entry.getEmbedding());
                matches.add(new EmbeddingMatch<>(score, entry.getId(), embedding, segment));
            }
        }

        matches.sort((a, b) -> Double.compare(b.score(), a.score()));
        return matches.subList(0, Math.min(maxResults, matches.size()));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        saveToFile();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private List<EmbeddingEntry> loadFromFile() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        try {
            String json = Files.readString(filePath);
            if (json.isBlank()) return new ArrayList<>();
            EmbeddingData data = objectMapper.readValue(json, EmbeddingData.class);
            return data.getEntries() != null ? data.getEntries() : new ArrayList<>();
        } catch (IOException e) {
            log.warn("Failed to load embeddings from {}: {}", filePath, e.getMessage());
            return new ArrayList<>();
        }
    }

    private synchronized void saveToFile() {
        try {
            Files.createDirectories(filePath.getParent());
            EmbeddingData data = new EmbeddingData();
            data.setEntries(new ArrayList<>(entries));
            data.setModelName("all-MiniLM-L6-v2-q");
            data.setTotalEntries(entries.size());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
        } catch (IOException e) {
            log.error("Failed to save embeddings to {}: {}", filePath, e.getMessage());
        }
    }

    public static class EmbeddingData {
        private String modelName;
        private int totalEntries;
        private List<EmbeddingEntry> entries;

        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public int getTotalEntries() { return totalEntries; }
        public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
        public List<EmbeddingEntry> getEntries() { return entries; }
        public void setEntries(List<EmbeddingEntry> entries) { this.entries = entries; }
    }

    public static class EmbeddingEntry {
        private String id;
        private String text;
        private List<Float> embedding;
        private Map<String, String> metadata;

        public EmbeddingEntry() {}

        public EmbeddingEntry(String id, String text, List<Float> embedding, Map<String, String> metadata) {
            this.id = id;
            this.text = text;
            this.embedding = embedding;
            this.metadata = metadata;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<Float> getEmbedding() { return embedding; }
        public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    }
}
