package integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        MockMultipartFile file = new MockMultipartFile("file", "test.pgn", "image/jpeg", "Test content".getBytes());

        // Perform the upload request without mocking
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message", is("Unsupported file type: image/jpeg")))
                .andExpect(jsonPath("$.status", is(415)));
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

        // Prepare the chat request using ObjectMapper
        String question = "What is the content?";
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("documentId", documentId);
        requestJson.put("question", question);
        String jsonRequest = objectMapper.writeValueAsString(requestJson);

        // Perform the chat request
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

        // Prepare the chat request using ObjectMapper
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("documentId", documentId);
        requestJson.put("question", question);
        String jsonRequest = objectMapper.writeValueAsString(requestJson);

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
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.putNull("documentId");
        requestJson.putNull("question");
        String jsonRequest = objectMapper.writeValueAsString(requestJson);

        // Perform the chat request expecting a 400 Bad Request
        mockMvc.perform(post("/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void testUploadDocument_PdfFile() throws Exception {
        // Read a small PDF file from the test resources
        byte[] pdfContent = Files.readAllBytes(Paths.get("src/test/resources/zajac.pdf"));
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", pdfContent);

        // Perform the upload request with the PDF file
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").exists())
                .andExpect(jsonPath("$.status", is("Success")));
    }

    @Test
    void testUploadDocument_UnsupportedMediaType() throws Exception {
        // Create a file with an unsupported media type
        MockMultipartFile file = new MockMultipartFile("file", "test.xyz", "application/xyz", "Test content".getBytes());

        // Perform the upload request expecting a 415 Unsupported Media Type
        mockMvc.perform(multipart("/upload")
                        .file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message", containsString("Unsupported file type: application/xyz")));
    }
}