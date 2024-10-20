package org.mkcoding.llm;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentRetrieverService {

    private final EmbeddingModel embeddingModel;
    private final Integer maxResults;
    private final Double minScore;

    public ContentRetrieverService(
            EmbeddingModel embeddingModel,
            @Value("${retriever.max.results}") Integer maxResults,
            @Value("${retriever.min.score}") Double minScore
    ) {
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    public List<Content> retrieveRelevantContents(String queryText, EmbeddingStore<TextSegment> embeddingStore) {
        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        return contentRetriever.retrieve(new Query(queryText));
    }
}