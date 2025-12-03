package com.minju.common.handler;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.CustomNotFoundException;
import com.minju.common.exception.CustomValidateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomNotFoundException.class)
    public ResponseEntity<CommonResponse<?>> handleCustomNotFoundException(CustomNotFoundException e) {
        log.error("CustomExceptionHandler CustomNotFoundException occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(404)
                .body(CommonResponse.error(404, 801, e.getMessage()));
    }

    @ExceptionHandler(CustomValidateException.class)
    public ResponseEntity<CommonResponse<?>> handleCustomValidateException(CustomValidateException e) {
        log.error("CustomExceptionHandler CustomValidateException occurred: {}", e.getMessage(), e);
        return ResponseEntity.status(400)
                .body(CommonResponse.error(400, 802, e.getMessage()));
    }
}