package com.minju.whitemonday;

import com.minju.whitemonday.common.dto.UserRoleEnum;
import com.minju.whitemonday.order.dto.OrderResponseDto;
import com.minju.whitemonday.order.entity.Order;
import com.minju.whitemonday.order.entity.OrderItem;
import com.minju.whitemonday.order.repository.OrderRepository;
import com.minju.whitemonday.product.entity.Product;
import com.minju.whitemonday.product.repository.ProductRepository;
import com.minju.whitemonday.user.entity.User;
import com.minju.whitemonday.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class OrderControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final long TEST_ORDER_ID = 1L;

    @BeforeEach
    public void setUp() {
        // 테스트용 제품 추가
        Product product = new Product();
        product.setTitle("Test Product");
        product.setDescription("Test Description");
        product.setPrice(200);
        product.setStock(50);
        product.setFlashSale(false);
        productRepository.save(product);

        // 테스트용 유저 10,000명 생성 및 주문 데이터 준비
        for (int i = 0; i < 10000; i++) {
            String username = "user" + i;
            String password = "password" + i;
            String email = "user" + i + "@example.com";

            // User 엔티티 생성 및 저장
            User user = new User(username, password, email, UserRoleEnum.USER);
            userRepository.save(user);

            // Order 엔티티 생성
            Order order = new Order(user, "PENDING", 100);

            // OrderItem 객체 생성 및 추가
            Product productFromDB = productRepository.findById(1L).orElseThrow(() -> new RuntimeException("Product not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);  // order 설정
            orderItem.setProduct(productFromDB);  // product 설정
            orderItem.setQuantity(1);  // 예시 수량
            orderItem.setPrice(100);  // 예시 가격

            // order에 orderItem 추가
            order.getOrderItems().add(orderItem);  // orderItems 리스트에 추가

            // 주문 저장
            orderRepository.save(order);  // Order 저장 시, 관련된 OrderItem도 함께 저장됩니다.
        }
    }

    private String generateJwtToken() {
        // 테스트용 임시 토큰 생성
        return "Bearer " + "test-jwt-token";
    }

    // 결제 API 부하 테스트
    @Test
    public void testCompletePayment() throws InterruptedException {
        int requestCount = 1000;  // 1000번의 요청을 보내기
        ExecutorService executorService = Executors.newFixedThreadPool(20);  // 최대 20개의 스레드 풀

        CountDownLatch latch = new CountDownLatch(requestCount);  // 모든 요청이 완료될 때까지 대기

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    // 요청 헤더에 JWT 토큰 추가
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", generateJwtToken());

                    // 요청 엔티티에 헤더 포함
                    HttpEntity<Void> entity = new HttpEntity<>(null, headers);

                    // 결제 완료 API 요청
                    ResponseEntity<OrderResponseDto> response = restTemplate.exchange(
                            "/api/orders/payment/complete/" + TEST_ORDER_ID,
                            HttpMethod.POST,
                            entity,
                            OrderResponseDto.class
                    );

                    assertNotNull(response);
                    assertEquals(200, response.getStatusCodeValue()); // HTTP 200 (OK) 응답 확인
                    if (response.getStatusCodeValue() != 200) {
                        System.out.println("Failed request");
                    }
                } finally {
                    latch.countDown();  // 요청이 끝나면 카운트 감소
                }
            });
        }

        // 모든 스레드가 끝날 때까지 기다립니다.
        latch.await();
        executorService.shutdown();  // 스레드 풀 종료
    }
}
