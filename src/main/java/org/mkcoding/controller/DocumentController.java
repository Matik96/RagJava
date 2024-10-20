package org.mkcoding.controller;

import jakarta.validation.Valid;
import org.mkcoding.dto.ChatRequestDto;
import org.mkcoding.dto.ChatResponseDto;
import org.mkcoding.dto.UploadResponseDto;
import org.mkcoding.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@Validated
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<UploadResponseDto> uploadDocument(@RequestParam("file") MultipartFile file) {
        Long documentId = service.uploadDocument(file);
        UploadResponseDto response = new UploadResponseDto(documentId, "Success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDto> chatWithDocument(@Valid @RequestBody ChatRequestDto requestDto) {
        String answer = service.chatWithDocument(requestDto.getDocumentId(), requestDto.getQuestion());
        ChatResponseDto response = new ChatResponseDto(answer);
        return ResponseEntity.ok(response);
    }

}