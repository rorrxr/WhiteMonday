package com.minju.common.exception;

public class CustomValidateException extends RuntimeException {

    public CustomValidateException() {
        super("The request is invalid, validation failed.");
    }

    public CustomValidateException(String message) {
        super(message);
    }

    public CustomValidateException(String message, Throwable cause) {
        super(message, cause);
    }
}