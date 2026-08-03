package com.akshara.api.bookimport.exception;

public class ExternalCatalogueException extends RuntimeException {
    public ExternalCatalogueException(String message) {
        super(message);
    }

    public ExternalCatalogueException(String message, Throwable cause) {
        super(message, cause);
    }
}
