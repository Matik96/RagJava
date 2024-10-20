package org.mkcoding.exception.exceptions;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String message) {
        super(message);
    }
}