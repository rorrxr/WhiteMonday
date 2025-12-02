package com.minju.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통 / 시스템 에러
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, 1000, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1001, "서버에 오류가 발생했습니다."),
    UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1002, "알 수 없는 오류가 발생했습니다."),

    // USER 도메인

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, 2000, "해당 유저를 찾을 수 없습니다."),

    // CART 도메인
    CART_NOT_FOUND(HttpStatus.NOT_FOUND, 3000, "장바구니를 찾을 수 없습니다."),
    CART_PRODUCT_ALREADY(HttpStatus.BAD_REQUEST, 3001, "해당 상품이 이미 장바구니에 존재합니다."),
    CART_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, 3002, "해당 장바구니 상품을 찾을 수 없습니다."),
    QUANTITY_INSUFFICIENT(HttpStatus.BAD_REQUEST, 3003, "원하시는 수량보다 재고가 적습니다."),
    CART_NOT_QUANTITY(HttpStatus.BAD_REQUEST, 3004, "해당 장바구니에는 상품이 없습니다."),

    // PRODUCT 도메인
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, 4000, "해당 상품을 찾을 수 없습니다."),
    PRODUCT_SOLD_OUT(HttpStatus.BAD_REQUEST, 4001, "품절상품이 포함되어있습니다."),
    PRODUCT_PRE_SALE(HttpStatus.BAD_REQUEST, 4002, "아직 판매준비중인 상품입니다."),
    PRODUCT_NOT_ORDER(HttpStatus.BAD_REQUEST, 4003, "구매할 수 없는 상품입니다."),

    //ORDER 도메인
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, 5000, "존재하지 않는 주문입니다."),
    NOT_YOUR_ORDER(HttpStatus.BAD_REQUEST, 5001, "주문정보와 아이디가 일치하지 않습니다."),
    ORDER_CANCELLED_FAILED(HttpStatus.BAD_REQUEST, 5002, "주문취소 불가능한 상태입니다."),

    // PAYMENT 도메인
    PAYMENT_ALREADY(HttpStatus.BAD_REQUEST, 6000, "이미 결제내역이 존재하는 주문입니다."),
    PAYMENT_ALREADY_COMPLETE(HttpStatus.BAD_REQUEST, 6001, "이미 결제완료된 주문입니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, 6002, "결제정보가 존재하지 않습니다."),
    INVALID_PAYMENT_STATUS(HttpStatus.BAD_REQUEST, 6003, "결제상태가 올바르지 않습니다."),
    FAILED_TIME_PAYMENT(HttpStatus.BAD_REQUEST, 6004, "시간초과로 결제가 취소됩니다."),
    FAILED_QUANTITY_PAYMENT(HttpStatus.BAD_REQUEST, 6005, "재고부족으로 결제가 취소됩니다."),
    FAILED_PAYMENT(HttpStatus.BAD_REQUEST, 6006, "결제 실패하셨습니다."),
    CANCELED_PAYMENT(HttpStatus.BAD_REQUEST, 6007, "결제 취소하셨습니다."),

    // REDIS / CACHE
    REDIS_NOT_FOUND(HttpStatus.BAD_REQUEST, 7000, "레디스에 저장된 정보를 찾을 수 없습니다.");

    private final HttpStatus httpStatus; // HTTP 응답 코드
    private final int code;              // 비즈니스 에러 코드
    private final String message;        // 기본 메시지
}