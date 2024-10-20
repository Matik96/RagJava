package org.mkcoding.service;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.mkcoding.exception.exceptions.DocumentNotFoundException;
import org.mkcoding.exception.exceptions.FileProcessingException;
import org.mkcoding.exception.exceptions.UnsupportedMediaTypeException;
import org.mkcoding.llm.ContentRetrieverService;
import org.mkcoding.llm.PromptBuilder;
import org.mkcoding.model.Document;
import org.mkcoding.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatLanguageModel;
    private final ContentRetrieverService contentRetriever;
    private final PromptBuilder promptBuilder;

    public DocumentService(DocumentRepository documentRepository, EmbeddingModel embeddingModel, ChatLanguageModel chatLanguageModel, ContentRetrieverService contentRetriever, PromptBuilder promptBuilder) {
        this.documentRepository = documentRepository;
        this.embeddingModel = embeddingModel;
        this.chatLanguageModel = chatLanguageModel;
        this.contentRetriever = contentRetriever;
        this.promptBuilder = promptBuilder;
    }

    public Long uploadDocument(MultipartFile file) {
        try {
            // Select the appropriate parser based on the file type
            DocumentParser documentParser = selectDocumentParser(file);

            // Split the document into segments
            DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
            List<TextSegment> segments = splitter.split(documentParser.parse(file.getInputStream()));

            // Embed the segments
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

            // Create a new embedding store for this document
            EmbeddingStore<TextSegment> documentEmbeddingStore = new InMemoryEmbeddingStore<>();
            documentEmbeddingStore.addAll(embeddings, segments);

            Document document = new Document(documentEmbeddingStore);
            documentRepository.save(document);

            return document.getId();
        } catch (IOException e) {
            throw new FileProcessingException("Failed to read file content");
        }
    }

    public String chatWithDocument(Long documentId, String question) {

        // Validate inputs
        if (documentId == null || documentId <= 0) {
            throw new IllegalArgumentException("Document ID must be a positive non-null value.");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be null or blank.");
        }
        log.info("Starting chatWithDocument for documentId: {}, question: '{}'", documentId, question);

        // Retrieve the embedding store for the given document ID
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));

        EmbeddingStore<TextSegment> documentEmbeddingStore = document.getDocumentEmbeddingStore();
        // Retrieve relevant segments based on the question
        List<Content> relevantSegments = contentRetriever.retrieveRelevantContents(question, documentEmbeddingStore);

        // Build the prompt
        String prompt = promptBuilder.buildPrompt(relevantSegments, question);

        // Generate the answer
        String answer = chatLanguageModel.generate(prompt);

        log.info("Completed chatWithDocument for documentId: {}", documentId);

        return answer;
    }

    private DocumentParser selectDocumentParser(MultipartFile file) throws UnsupportedMediaTypeException {
        String contentType = file.getContentType();

        // LangChain4j supports parsing multiple document types:
        // text, pdf, doc, xls, ppt.
        if ("application/pdf".equals(contentType) || "text/plain".equals(contentType) || "application/msword".equals(contentType) || "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(contentType)) {
            return new TextDocumentParser();
        } else {
            throw new UnsupportedMediaTypeException("Unsupported file type: " + contentType);
        }
    }
}
