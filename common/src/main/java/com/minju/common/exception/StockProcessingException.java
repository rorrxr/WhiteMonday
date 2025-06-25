package com.minju.common.exception;

public class StockProcessingException extends RuntimeException {
    public StockProcessingException(String message) {
        super(message);
    }
}