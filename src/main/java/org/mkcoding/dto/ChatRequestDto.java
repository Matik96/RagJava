package org.mkcoding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public class ChatRequestDto {

    @NotNull(message = "Document ID cannot be empty")
    private Long documentId;

    @NotBlank(message = "Question cannot be empty")
    private String question;

}