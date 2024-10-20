package org.mkcoding.model;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@NoArgsConstructor(force = true)
public class Document {

    private static final AtomicLong counter = new AtomicLong(1); // Starts at 1
    private final Long id;
    private final EmbeddingStore<TextSegment> documentEmbeddingStore;

    public Document(EmbeddingStore<TextSegment> documentEmbeddingStore) {
        //UUID.randomUUID().toString(); -> better for bigger apps (unique ids)
        this.id = counter.getAndIncrement();
        this.documentEmbeddingStore = documentEmbeddingStore;
    }
}


