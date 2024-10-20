package integration;

import org.junit.jupiter.api.Test;
import org.mkcoding.exception.exceptions.DocumentNotFoundException;
import org.mkcoding.exception.exceptions.UnsupportedMediaTypeException;
import org.mkcoding.run.Main;
import org.mkcoding.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(classes = Main.class)
class DocumentServiceIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Test
    void testUploadAndChatWorkflow() throws Exception {
        // Upload the document
        String content = "This is an integration test document about testing.";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
        Long documentId = documentService.uploadDocument(file);

        assertNotNull(documentId);

        // Chat with the document
        String question = "What is this document about?";
        String answer = documentService.chatWithDocument(documentId, question);

        assertNotNull(answer);
        assertTrue(answer.toLowerCase().contains("testing"), "The answer should mention 'testing'");
    }

    @Test
    void testChatWithNonExistentDocument() {
        Long invalidDocumentId = 9999L;
        String question = "What is the content?";

        assertThrows(DocumentNotFoundException.class, () -> {
            documentService.chatWithDocument(invalidDocumentId, question);
        });
    }

    @Test
    void testUploadDocumentWithInvalidFile() {
        // Arrange
        MultipartFile invalidFile = new MockMultipartFile("file", "test.xyz", "application/xyz", new byte[]{});

        // Act & Assert
        assertThrows(UnsupportedMediaTypeException.class, () -> {
            documentService.uploadDocument(invalidFile);
        });
    }
}
