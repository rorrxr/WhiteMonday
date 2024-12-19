package com.minju.whitemonday.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData<T> {
    private static final String SUCCESS_STATUS = "success";
    private static final String FAILURE_STATUS = "failure";
    private static final String EXPIRED_STATUS = "expired";
    private static final String ERROR_STATUS = "error";

    private static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
    private static final String FAILURE_MESSAGE = "입력 또는 요청 매개변수의 오류로 인해 요청이 실패했습니다.";
    private static final String EXPIRED_MESSAGE = "요청이 만료되었거나 데이터가 더 이상 유효하지 않아 요청이 실패했습니다.";
    private static final String ERROR_MESSAGE = "요청을 처리하는 중 예상치 못한 오류가 발생했습니다.";

    private String status;
    private String message;
    private T data;

    public static <T> ResponseData<T> success() {
        return new ResponseData<>(SUCCESS_STATUS, SUCCESS_MESSAGE, null);
    }

    public static <T> ResponseData<T> success(T data) {
        return new ResponseData<>(SUCCESS_STATUS, SUCCESS_MESSAGE, data);
    }

    public static <T> ResponseData<T> success(String message) {
        return new ResponseData<>(SUCCESS_STATUS, message, null);
    }

    public static <T> ResponseData<T> success(String message, T data) {
        return new ResponseData<>(SUCCESS_STATUS, message, data);
    }

    public static <T> ResponseData<T> failure() {
        return new ResponseData<>(FAILURE_STATUS, FAILURE_MESSAGE, null);
    }

    public static <T> ResponseData<T> failure(String message) {
        return new ResponseData<>(FAILURE_STATUS, message, null);
    }

    public static <T> ResponseData<T> expired() {
        return new ResponseData<>(EXPIRED_STATUS, EXPIRED_MESSAGE, null);
    }

    public static <T> ResponseData<T> expired(String message) {
        return new ResponseData<>(EXPIRED_STATUS, message, null);
    }

    public static <T> ResponseData<T> error() {
        return new ResponseData<>(ERROR_STATUS, ERROR_MESSAGE, null);
    }

    public static <T> ResponseData<T> error(String message) {
        return new ResponseData<>(ERROR_STATUS, message, null);
    }

    public static <T> ResponseData<T> custom(String status, String message, T data) {
        return new ResponseData<>(status, message, data);
    }
}
