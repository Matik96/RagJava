package service;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mkcoding.exception.exceptions.DocumentNotFoundException;
import org.mkcoding.exception.exceptions.FileProcessingException;
import org.mkcoding.exception.exceptions.UnsupportedMediaTypeException;
import org.mkcoding.llm.ContentRetrieverService;
import org.mkcoding.llm.PromptBuilder;
import org.mkcoding.model.Document;
import org.mkcoding.repository.DocumentRepository;
import org.mkcoding.service.DocumentService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private DocumentRepository documentRepository;
    private DocumentService documentService;
    private EmbeddingModel embeddingModel;
    private ContentRetrieverService contentRetrieverService;
    private ChatLanguageModel chatLanguageModel;
    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        documentRepository = mock(DocumentRepository.class);
        embeddingModel = mock(EmbeddingModel.class);
        chatLanguageModel = mock(ChatLanguageModel.class);
        contentRetrieverService = mock(ContentRetrieverService.class);
        promptBuilder = mock(PromptBuilder.class);
        documentService = new DocumentService(documentRepository, embeddingModel, chatLanguageModel, contentRetrieverService, promptBuilder);
    }

    @Test
    void testUploadDocument_Success() throws IOException {
        String content = "This is a test document.";
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());

        List<Embedding> embeddings = Arrays.asList(
                new Embedding(new float[]{0.5f, 0.2f, 0.3f})
        );

        Response<List<Embedding>> response = mock(Response.class);
        when(response.content()).thenReturn(embeddings);

        when(embeddingModel.embedAll(anyList())).thenReturn(response);
        Long documentId = documentService.uploadDocument(file);
        assertNotNull(documentId);
    }

    @Test
    void testUploadDocument_UnsupportedMediaType() {
        MultipartFile file = new MockMultipartFile("file", "test.xyz", "application/xyz", new byte[]{});

        assertThrows(UnsupportedMediaTypeException.class, () -> documentService.uploadDocument(file));
    }

    @Test
    void testUploadDocument_FileProcessingException() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("text/plain");
        when(file.getInputStream()).thenThrow(new IOException("IO Error"));

        assertThrows(FileProcessingException.class, () -> documentService.uploadDocument(file));
    }

    @Test
    void testChatWithDocument_Success() {
        // Arrange
        Long documentId = 1L;
        String question = "What is the content?";

        // Mock DocumentRepository to return a mock Document
        Document document = mock(Document.class); // Mock the Document object
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class); // Mock EmbeddingStore

        // Set up the mocks for repository and document behavior
        when(documentRepository.findById(documentId)).thenReturn(Optional.ofNullable(document));
        when(document.getDocumentEmbeddingStore()).thenReturn(embeddingStore);

        // Mock the ContentRetrieverService and ChatLanguageModel behavior
        List<Content> contents = Collections.singletonList(Content.from("This is the content."));
        when(contentRetrieverService.retrieveRelevantContents(eq(question), eq(embeddingStore))).thenReturn(contents);
        when(promptBuilder.buildPrompt(contents, question)).thenReturn("You are an assistant that answers questions based on the provided document.\n\n Context: ");
        when(chatLanguageModel.generate(anyString())).thenReturn("This is the answer.");


        // Act
        String answer = documentService.chatWithDocument(documentId, question);

        // Assert
        assertEquals("This is the answer.", answer);
    }


    @Test
    void testChatWithDocument_DocumentNotFound() {
        // Arrange
        Long documentId = 999L;
        String question = "What is the content?";

        // Mock DocumentRepository to return a mock Document
        Document document = mock(Document.class); // Mock the Document object
        EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class); // Mock EmbeddingStore

        // Set up the mocks for repository and document behavior
        when(documentRepository.findById(documentId)).thenReturn(Optional.ofNullable(document));
        when(document.getDocumentEmbeddingStore()).thenReturn(null);

        // Act & Assert
        assertThrows(DocumentNotFoundException.class, () -> documentService.chatWithDocument(documentId, question));
    }

    @Test
    void testUploadDocument_EmptyFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[]{});

        // Act & Assert
        assertThrows(BlankDocumentException.class, () -> documentService.uploadDocument(file));
    }

    @Test
    void testUploadDocument_NullContentType() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "test.txt", null, "Test content".getBytes());

        // Act & Assert
        assertThrows(UnsupportedMediaTypeException.class, () -> documentService.uploadDocument(file));
    }

    @Test
    void testChatWithDocument_EmptyQuestion() {
        // Arrange
        Long documentId = 1L;
        String question = "";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentService.chatWithDocument(documentId, question));
    }

    @Test
    void testChatWithDocument_NullQuestion() {
        // Arrange
        Long documentId = 1L;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentService.chatWithDocument(documentId, null));
    }

    @Test
    void testChatWithDocument_NullDocumentId() {
        // Arrange
        String question = "What is the content?";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentService.chatWithDocument(null, question));
    }

    @Test
    void testChatWithDocument_NegativeDocumentId() {
        // Arrange
        Long documentId = -1L;
        String question = "What is the content?";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> documentService.chatWithDocument(documentId, question));
    }

}
