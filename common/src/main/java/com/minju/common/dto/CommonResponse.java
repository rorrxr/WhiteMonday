package com.minju.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommonResponse<T> {

    private boolean success;      // 성공 여부
    private int status;           // HTTP status code
    private int code;             // 비즈니스 에러 코드 (성공 시 0)
    private String message;       // 메시지
    private T data;               // 응답 데이터
    private LocalDateTime timestamp;

    // 성공 응답

    public static <T> CommonResponse<T> success(T data) {
        return success("The request was successful", data);
    }

    public static <T> CommonResponse<T> success(String message, T data) {
        return CommonResponse.<T>builder()
                .success(true)
                .status(200)
                .code(0) // 성공은 0으로 통일
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static CommonResponse<Void> success() {
        return success(null);
    }

    // 에러 응답

    public static <T> CommonResponse<T> error(int status, int code, String message) {
        return error(status, code, message, null);
    }

    public static <T> CommonResponse<T> error(int status, int code, String message, T data) {
        return CommonResponse.<T>builder()
                .success(false)
                .status(status)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}