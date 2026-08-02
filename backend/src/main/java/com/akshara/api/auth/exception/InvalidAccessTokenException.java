package com.akshara.api.auth.exception;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Access token is no longer valid");
    }
}