package com.minju.common.exception;

public class ServiceUnavailableException  extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
