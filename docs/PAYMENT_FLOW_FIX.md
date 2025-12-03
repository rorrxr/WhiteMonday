# 결제 플로우 버그 수정 보고서

## 개요

결제 시스템 분석 중 발견된 컴파일 에러를 수정하였습니다.

**수정 일자:** 2025-12-27

---

## 문제 요약

`PaymentRequestedEvent.PaymentFailedEvent` 내부 클래스를 참조하고 있었으나, 실제로는 `PaymentFailedEvent`가 별도의 독립 클래스로 존재하여 **컴파일 에러**가 발생하는 문제였습니다.

### 영향받은 파일 (총 6개)

**결제 플로우 핵심 수정:**
1. `order-service/.../saga/SagaOrchestrator.java`
2. `payment-service/.../saga/PaymentSagaHandler.java`
3. `payment-service/.../outbox/OutboxEventPublisher.java`

**빌드 오류 추가 수정:**
4. `common/.../handler/CustomExceptionHandler.java`
5. `order-service/.../controller/OrderController.java`
6. `order-service/.../outbox/OutboxEventPublisher.java`

---

## 수정 내역

### 1. SagaOrchestrator.java

**파일 경로:** `order-service/src/main/java/com/minju/order/saga/SagaOrchestrator.java`

#### 1-1. Import 문 수정

**Before:**
```java
import com.minju.common.kafka.*;
import com.minju.common.kafka.order.OrderCancelledEvent;
import com.minju.common.kafka.order.OrderCompletedEvent;
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.kafka.stock.StockReservationFailedEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
```

**After:**
```java
import com.minju.common.kafka.order.OrderCancelledEvent;
import com.minju.common.kafka.order.OrderCompletedEvent;
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentFailedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.kafka.stock.StockReservationFailedEvent;
import com.minju.common.kafka.stock.StockRestoreEvent;
import com.minju.common.kafka.stock.StockReservedEvent;
```

**변경 사항:**
- 와일드카드 import (`com.minju.common.kafka.*`) 제거
- `PaymentFailedEvent` 명시적 import 추가
- `StockReservedEvent` 명시적 import 추가

---

#### 1-2. handlePaymentFailed 메서드 파라미터 타입 수정

**Before:**
```java
@KafkaListener(topics = "payment-failed-topic", groupId = "order-saga-group")
@Transactional
public void handlePaymentFailed(PaymentRequestedEvent.PaymentFailedEvent event) {
    log.error("결제 실패 수신: orderId={}, reason={}",
            event.getOrderId(), event.getReason());
    handlePaymentFailure(event);
}
```

**After:**
```java
@KafkaListener(topics = "payment-failed-topic", groupId = "order-saga-group")
@Transactional
public void handlePaymentFailed(PaymentFailedEvent event) {
    log.error("결제 실패 수신: orderId={}, reason={}",
            event.getOrderId(), event.getReason());
    handlePaymentFailure(event);
}
```

**변경 사항:**
- `PaymentRequestedEvent.PaymentFailedEvent` → `PaymentFailedEvent`

---

#### 1-3. handlePaymentFailure 메서드 내 타입 체크 수정

**Before:**
```java
if (event instanceof PaymentCompletedEvent pce) {
    orderId = pce.getOrderId();
    productId = pce.getProductId();
    quantity = pce.getQuantity();
    reason = "결제 실패";
} else if (event instanceof PaymentRequestedEvent.PaymentFailedEvent pfe) {
    orderId = pfe.getOrderId();
    productId = pfe.getProductId();
    quantity = pfe.getQuantity();
    reason = pfe.getReason();
} else {
    log.error("알 수 없는 이벤트 타입: {}", event.getClass());
    return;
}
```

**After:**
```java
if (event instanceof PaymentCompletedEvent pce) {
    orderId = pce.getOrderId();
    productId = pce.getProductId();
    quantity = pce.getQuantity();
    reason = "결제 실패";
} else if (event instanceof PaymentFailedEvent pfe) {
    orderId = pfe.getOrderId();
    productId = pfe.getProductId();
    quantity = pfe.getQuantity();
    reason = pfe.getReason();
} else {
    log.error("알 수 없는 이벤트 타입: {}", event.getClass());
    return;
}
```

**변경 사항:**
- `PaymentRequestedEvent.PaymentFailedEvent` → `PaymentFailedEvent`

---

### 2. PaymentSagaHandler.java

**파일 경로:** `payment-service/src/main/java/com/minju/paymentservice/saga/PaymentSagaHandler.java`

#### 2-1. Import 문 수정

**Before:**
```java
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.outbox.OutboxEventPublisher;
```

**After:**
```java
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentFailedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.paymentservice.outbox.OutboxEventPublisher;
```

**변경 사항:**
- `PaymentFailedEvent` import 추가

---

#### 2-2. publishPaymentFailedEvent 메서드 수정

**Before:**
```java
private void publishPaymentFailedEvent(PaymentRequestedEvent event, String reason) {
    try {
        PaymentRequestedEvent.PaymentFailedEvent failEvent = PaymentRequestedEvent.PaymentFailedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .reason(reason)
                .status("PAYMENT_FAILED")
                .build();
        // ...
    }
}
```

**After:**
```java
private void publishPaymentFailedEvent(PaymentRequestedEvent event, String reason) {
    try {
        PaymentFailedEvent failEvent = PaymentFailedEvent.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .productId(event.getProductId())
                .quantity(event.getQuantity())
                .reason(reason)
                .status("PAYMENT_FAILED")
                .build();
        // ...
    }
}
```

**변경 사항:**
- `PaymentRequestedEvent.PaymentFailedEvent` → `PaymentFailedEvent`

---

### 3. PaymentService OutboxEventPublisher.java

**파일 경로:** `payment-service/src/main/java/com/minju/paymentservice/outbox/OutboxEventPublisher.java`

#### 3-1. Import 문 수정

**Before:**
```java
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.kafka.payment.PaymentCompletedEvent;
```

**After:**
```java
import com.minju.common.kafka.payment.PaymentCompletedEvent;
import com.minju.common.kafka.payment.PaymentFailedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
```

#### 3-2. getEventClass 메서드 수정

**Before:**
```java
case "PAYMENT_FAILED" ->
        PaymentRequestedEvent.PaymentFailedEvent.class;
```

**After:**
```java
case "PAYMENT_FAILED" ->
        PaymentFailedEvent.class;
```

---

### 4. CustomExceptionHandler.java (기존 빌드 오류 수정)

**파일 경로:** `common/src/main/java/com/minju/common/handler/CustomExceptionHandler.java`

**Before:**
```java
@ExceptionHandler(CustomNotFoundException.class)
public ResponseEntity<CommonResponse<?>> handleCustomNotFoundException(CustomNotFoundException e) {
    return ResponseEntity.status(801)
            .body(CommonResponse.error(801));  // 메서드 시그니처 불일치
}

@ExceptionHandler(CustomValidateException.class)
public ResponseEntity<CommonResponse<?>> handleCustomValidateException(CustomValidateException e) {
    return ResponseEntity.status(802)
            .body(CommonResponse.error(802));  // 메서드 시그니처 불일치
}
```

**After:**
```java
@ExceptionHandler(CustomNotFoundException.class)
public ResponseEntity<CommonResponse<?>> handleCustomNotFoundException(CustomNotFoundException e) {
    return ResponseEntity.status(404)
            .body(CommonResponse.error(404, 801, e.getMessage()));
}

@ExceptionHandler(CustomValidateException.class)
public ResponseEntity<CommonResponse<?>> handleCustomValidateException(CustomValidateException e) {
    return ResponseEntity.status(400)
            .body(CommonResponse.error(400, 802, e.getMessage()));
}
```

**변경 사항:**
- `CommonResponse.error(int)` → `CommonResponse.error(int status, int code, String message)` 시그니처에 맞게 수정
- HTTP 상태 코드도 적절하게 변경 (801 → 404, 802 → 400)

---

### 5. OrderController.java (기존 빌드 오류 수정)

**파일 경로:** `order-service/src/main/java/com/minju/order/controller/OrderController.java`

**Before:**
```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(CommonResponse.success(
                HttpStatus.CREATED.value(),
                "주문이 정상적으로 생성되었습니다.",
                response
        ));
```

**After:**
```java
return ResponseEntity.status(HttpStatus.CREATED)
        .body(CommonResponse.success(
                "주문이 정상적으로 생성되었습니다.",
                response
        ));
```

**변경 사항:**
- `CommonResponse.success(int, String, T)` → `CommonResponse.success(String, T)` 시그니처에 맞게 수정

---

### 6. OrderService OutboxEventPublisher.java (기존 빌드 오류 수정)

**파일 경로:** `order-service/src/main/java/com/minju/order/outbox/OutboxEventPublisher.java`

#### 6-1. Import 문 수정

**Before:**
```java
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.kafka.stock.StockReservationRequestEvent;
```

**After:**
```java
import com.minju.common.kafka.order.OrderCreatedEvent;
import com.minju.common.kafka.payment.PaymentRequestedEvent;
import com.minju.common.kafka.stock.StockReservationRequestEvent;
```

#### 6-2. getEventClass 메서드 수정

**Before:**
```java
case "ORDER_CREATED" ->
        com.minju.common.kafka.OrderCreatedEvent.class;  // 잘못된 패키지 경로
```

**After:**
```java
case "ORDER_CREATED" ->
        OrderCreatedEvent.class;  // 올바른 import 사용
```

**변경 사항:**
- `com.minju.common.kafka.OrderCreatedEvent` → `com.minju.common.kafka.order.OrderCreatedEvent`

---

## 수정 후 결제 플로우

```
1. 주문 생성 (PENDING)
   └─ StockReservationRequestEvent → Outbox

2. 재고 예약 (Product Service)
   ├─ 성공 → StockReservedEvent
   └─ 실패 → StockReservationFailedEvent

3. 재고 예약 성공 시 → 결제 요청
   └─ PaymentRequestedEvent → Outbox

4. 결제 처리 (Payment Service)
   ├─ 성공 → PaymentCompletedEvent (success=true)
   └─ 실패 → PaymentFailedEvent ← [수정됨]

5. 결제 결과 처리 (Order Service)
   ├─ 성공 → COMPLETED
   └─ 실패 → PAYMENT_FAILED + StockRestoreEvent (보상 트랜잭션)
```

---

## 이벤트 클래스 구조

### 현재 이벤트 클래스 위치

```
common/src/main/java/com/minju/common/kafka/
├── order/
│   ├── OrderCancelledEvent.java
│   ├── OrderCompletedEvent.java
│   └── OrderCreatedEvent.java
├── payment/
│   ├── PaymentCompletedEvent.java
│   ├── PaymentFailedEvent.java        ← 독립 클래스
│   ├── PaymentManualProcessingEvent.java
│   └── PaymentRequestedEvent.java
└── stock/
    ├── StockReservationFailedEvent.java
    ├── StockReservationRequestEvent.java
    ├── StockReservedEvent.java
    └── StockRestoreEvent.java
```

---

## 검증 방법

1. **컴파일 테스트** ✅ 통과
   ```bash
   ./gradlew :order-service:compileJava :payment-service:compileJava
   ```

   ```
   BUILD SUCCESSFUL in 26s
   5 actionable tasks: 1 executed, 4 up-to-date
   ```

2. **통합 테스트**
   - 결제 성공 시나리오: 주문 → 재고 예약 → 결제 → 완료
   - 결제 실패 시나리오: 주문 → 재고 예약 → 결제 실패 → 재고 복구

---

## 추가 권장 사항

### 아직 해결되지 않은 잠재적 문제

1. **다중 상품 주문 시 Saga 동기화 문제**
   - 현재 각 상품별로 별도의 `StockReservationRequestEvent`가 발행됨
   - 모든 상품의 재고 예약이 완료된 후 결제를 진행해야 함
   - 해결 방안: 주문 단위로 재고 예약 이벤트를 통합하거나, 재고 예약 완료 카운터 도입

2. **Outbox 정리 전략 부재**
   - `PUBLISHED` 상태의 이벤트가 계속 누적됨
   - 주기적인 삭제 스케줄러 필요
