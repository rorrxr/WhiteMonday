package com.minju.common.exception;

public class InsufficientStockException  extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}