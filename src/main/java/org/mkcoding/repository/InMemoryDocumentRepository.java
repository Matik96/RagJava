package org.mkcoding.repository;

import org.mkcoding.model.Document;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryDocumentRepository implements DocumentRepository  {
    private final ConcurrentHashMap<Long, Document> documents = new ConcurrentHashMap<>();

    public void save(Document document) {
        documents.put(document.getId(), document);
    }

    public Optional<Document> findById(Long id) {
        return Optional.ofNullable(documents.get(id));
    }
}
