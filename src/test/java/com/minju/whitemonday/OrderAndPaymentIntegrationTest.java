package com.minju.whitemonday;

import com.minju.whitemonday.order.dto.OrderRequestDto;
import com.minju.whitemonday.order.dto.OrderResponseDto;
import com.minju.whitemonday.order.repository.OrderRepository;
import com.minju.whitemonday.order.service.OrderService;
import com.minju.whitemonday.product.controller.ProductController;
import com.minju.whitemonday.product.dto.ProductResponseDto;
import com.minju.whitemonday.product.repository.ProductRepository;
import com.minju.whitemonday.product.service.ProductService;
import com.minju.whitemonday.user.service.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ProductController.class)
public class OrderAndPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ProductService productService;

    @MockBean
    private UserDetailsImpl userDetailsImpl; // 사용자 정보 mock

    @MockBean
    private ProductRepository productRepository; // ProductRepository mock 추가

    private String generateJwtToken() {
        // 테스트용 임시 토큰 생성
        return "Bearer " + "test-jwt-token";
    }

    @BeforeEach
    public void setup() {
        // ProductController의 두 번째 매개변수인 ProductRepository를 추가하여 mockMvc 초기화
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService, productRepository))
                .build();
    }

    @Test
    public void testCreateOrderAndPayment() throws Exception {
        // 주문할 상품과 수량 설정
        OrderRequestDto orderRequestDto = new OrderRequestDto();
        orderRequestDto.setItems(List.of(
                new OrderRequestDto.Item(1L, 2)  // 상품 ID 1번, 수량 2
        ));

        // 상품 목록과 주문 처리 mock
        ProductResponseDto productResponseDto = new ProductResponseDto(
                1L, "Test Product", "Test Description", 100, 50, false, null, null, null
        );

        given(productService.getAllProducts()).willReturn(List.of(productResponseDto)); // 상품 목록 mock

        // 주문 생성 후 결제 실패/성공 시나리오를 처리하기 위해 성공과 실패를 랜덤으로 설정
        given(orderService.createOrder(orderRequestDto, userDetailsImpl.getUser()))
                .willReturn(new OrderResponseDto(null)); // 주문 생성 후

        // 결제 처리 시 20% 확률로 실패
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 2}]}") // 요청 바디
                        .header("Authorization", generateJwtToken()))  // JWT 토큰 추가
                .andExpect(status().isCreated())  // 주문 생성이 성공하면 상태 코드 201
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_FAILED"))  // 결제 실패시 실패 처리
                .andExpect(jsonPath("$.orderStatus").value("PAYMENT_COMPLETED"));  // 결제 성공시 완료 처리
    }
}
