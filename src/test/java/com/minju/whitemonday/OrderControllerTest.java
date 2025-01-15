package com.minju.whitemonday;

import com.minju.whitemonday.common.dto.UserRoleEnum;
import com.minju.whitemonday.common.util.JwtUtil;
import com.minju.whitemonday.order.dto.OrderResponseDto;
import com.minju.whitemonday.order.entity.Order;
import com.minju.whitemonday.order.entity.OrderItem;
import com.minju.whitemonday.order.repository.OrderRepository;
import com.minju.whitemonday.product.entity.Product;
import com.minju.whitemonday.product.repository.ProductRepository;
import com.minju.whitemonday.user.entity.User;
import com.minju.whitemonday.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"spring.profiles.active=test"})
public class OrderControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Mock
    private JwtUtil jwtUtil;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.openMocks(this);
    }

    private final int TOTAL_USERS = 10000;
    private final int PAYMENT_ATTEMPTS = 8000;

    @BeforeEach
    public void setUp() {
        // Mock JWT Util behavior
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString())).thenReturn("user0");

        // Initialize product
        Product product = new Product("Test Product", "Test Description", 200, 10000);
        productRepository.save(product);

        // Initialize users and orders
        for (int i = 0; i < TOTAL_USERS; i++) {
            User user = new User("user" + i, "password" + i, "user" + i + "@example.com", UserRoleEnum.USER);
            userRepository.save(user);

            Order order = new Order(user, "PENDING", product.getPrice());
            orderRepository.save(order);
        }
    }

    @Test
    public void testPaymentScreenAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(TOTAL_USERS);

        for (int i = 0; i < TOTAL_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer test-jwt-token");
                    HttpEntity<Void> entity = new HttpEntity<>(null, headers);

                    ResponseEntity<Void> response = restTemplate.exchange(
                            "/api/orders/payment/screen/user" + userId,
                            HttpMethod.GET,
                            entity,
                            Void.class
                    );

                    assertEquals(200, response.getStatusCodeValue());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    @Test
    public void testPaymentAttempts() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(PAYMENT_ATTEMPTS);

        for (int i = 0; i < PAYMENT_ATTEMPTS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer test-jwt-token");
                    HttpEntity<Void> entity = new HttpEntity<>(null, headers);

                    ResponseEntity<OrderResponseDto> response = restTemplate.exchange(
                            "/api/orders/payment/complete/user" + userId,
                            HttpMethod.POST,
                            entity,
                            OrderResponseDto.class
                    );

                    assertEquals(200, response.getStatusCodeValue());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
    }

    @Test
    public void testRemainingStockApi() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(2);

        // Before flash sale starts
        executor.submit(() -> {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer test-jwt-token");
                HttpEntity<Void> entity = new HttpEntity<>(null, headers);

                ResponseEntity<Integer> response = restTemplate.exchange(
                        "/api/products/stock/1",
                        HttpMethod.GET,
                        entity,
                        Integer.class
                );

                assertNotNull(response.getBody());
                System.out.println("Stock before flash sale: " + response.getBody());
            } finally {
                latch.countDown();
            }
        });

        // After flash sale starts
        executor.submit(() -> {
            try {
                Thread.sleep(5000); // Simulate delay for flash sale
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer test-jwt-token");
                HttpEntity<Void> entity = new HttpEntity<>(null, headers);

                ResponseEntity<Integer> response = restTemplate.exchange(
                        "/api/products/stock/1",
                        HttpMethod.GET,
                        entity,
                        Integer.class
                );

                assertNotNull(response.getBody());
                System.out.println("Stock after flash sale: " + response.getBody());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();
    }
}


