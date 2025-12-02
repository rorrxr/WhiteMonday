package com.minju.common.handler;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.BusinessException;
import com.minju.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 바인딩 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CommonResponse<?>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException", e);

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        ErrorCode.INVALID_REQUEST.getCode(),
                        message
                ));
    }

    // 단순 바인딩 실패 (타입 매칭 등)
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<CommonResponse<?>> handleBindException(BindException e) {
        log.error("BindException", e);

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        ErrorCode.INVALID_REQUEST.getCode(),
                        message
                ));
    }

    // 쿼리 파라미터 타입 오류 등
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<CommonResponse<?>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {

        log.error("MethodArgumentTypeMismatchException", e);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        ErrorCode.INVALID_REQUEST.getCode(),
                        "요청 파라미터의 타입이 올바르지 않습니다."
                ));
    }

    // 비즈니스 예외
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<CommonResponse<?>> handleBusinessException(BusinessException e) {
        log.error("BusinessException", e);

        return ResponseEntity
                .status(e.getHttpStatus())
                .body(CommonResponse.error(
                        e.getHttpStatus().value(),
                        e.getCode(),
                        e.getMessage()
                ));
    }

    // 그 외 나머지
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<CommonResponse<?>> handleException(Exception e) {
        log.error("Unhandled Exception", e);

        ErrorCode error = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(error.getHttpStatus())
                .body(CommonResponse.error(
                        error.getHttpStatus().value(),
                        error.getCode(),
                        error.getMessage()
                ));
    }
}
