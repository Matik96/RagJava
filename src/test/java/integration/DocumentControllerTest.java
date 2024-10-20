package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mkcoding.dto.ChatRequestDto;
import org.mkcoding.run.Main;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@SpringBootTest(classes = Main.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testUploadDocument_success() throws Exception {
        // Create a text file for upload
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "Test content".getBytes());

        // Perform the upload request without mocking
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").exists())
                .andExpect(jsonPath("$.status", is("Success")));
    }
    @Test
    void testUploadDocument_notText() throws Exception {
        // Create a text file for upload
        MockMultipartFile file = new MockMultipartFile("file", "test.pgn", "text/plain", "Test content".getBytes());

        // Perform the upload request without mocking
        mockMvc.perform(multipart("/upload")
                        .file(null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("Success")));
    }

    @Test
    void testChatWithDocument() throws Exception {
        // First, upload a document to get a valid document ID
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "This is a test document content.".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").exists())
                .andReturn();

        // Extract the documentId from the upload response
        String uploadResponse = uploadResult.getResponse().getContentAsString();
        Long documentId = JsonPath.parse(uploadResponse).read("$.documentId", Long.class);

        // Prepare the chat request
        String question = "What is the content?";
        ChatRequestDto requestDto = new ChatRequestDto(documentId, question);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // Perform the chat request without mocking
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").exists());
    }

    @Test
    void testChatWithDocument_DocumentNotFound() throws Exception {
        // Use a non-existent document ID
        Long documentId = 9999L;
        String question = "What is the content?";

        // Prepare the chat request
        ChatRequestDto requestDto = new ChatRequestDto(documentId, question);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // Perform the chat request expecting a 404 Not Found
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Document not found with ID: " + documentId)));
    }

    @Test
    void testChatWithDocument_InvalidRequest() throws Exception {
        // Prepare an invalid chat request with null fields
        ChatRequestDto requestDto = new ChatRequestDto(null, null);
        String jsonRequest = objectMapper.writeValueAsString(requestDto);

        // Perform the chat request expecting a 400 Bad Request
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))));
    }

    @Test
    void testUploadDocument_PdfFile() throws Exception {
        // Read a small PDF file from the test resources
        byte[] pdfContent = Files.readAllBytes(Paths.get("src/test/resources/test.pdf"));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);

        // Perform the upload request with the PDF file
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").exists())
                .andExpect(jsonPath("$.message", is("Success")));
    }

    @Test
    void testUploadDocument_UnsupportedMediaType() throws Exception {
        // Create a file with an unsupported media type
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "application/xyz", "Test content".getBytes());

        // Perform the upload request expecting a 415 Unsupported Media Type
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message", containsString("Unsupported media type")));
    }
}
