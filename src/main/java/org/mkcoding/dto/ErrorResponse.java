package org.mkcoding.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public class ErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;

}