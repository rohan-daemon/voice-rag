package com.voicerag.service;

import com.voicerag.model.SearchResult;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchService {
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final IndexService indexService;

    public SearchService(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore, IndexService indexService) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.indexService = indexService;
    }

    public List<SearchResult> search(String query, int maxResults) {
        if (!indexService.isIndexed()) {
            return List.of();
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults);

        return matches.stream()
                .map(match -> new SearchResult(
                        match.embedded().text(),
                        match.embedded().metadata().getString("source"),
                        match.score()
                ))
                .toList();
    }
}
