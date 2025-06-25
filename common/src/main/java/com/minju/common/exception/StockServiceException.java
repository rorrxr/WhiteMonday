package com.minju.common.exception;

public class StockServiceException extends RuntimeException {
    public StockServiceException(String message) {
        super(message);
    }
}