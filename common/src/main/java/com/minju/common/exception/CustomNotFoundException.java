package com.minju.common.exception;

public class CustomNotFoundException extends RuntimeException {

    public CustomNotFoundException() {
        super("The requested resource could not be found.");
    }

    public CustomNotFoundException(String message) {
        super(message);
    }

    public CustomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}