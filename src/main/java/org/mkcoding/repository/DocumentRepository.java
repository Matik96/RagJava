package org.mkcoding.repository;

import org.mkcoding.model.Document;

import java.util.Optional;

public interface DocumentRepository {


    void save(Document document);

    Optional<Document> findById(Long id);
}