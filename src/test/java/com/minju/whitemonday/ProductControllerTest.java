package com.minju.whitemonday;

import com.minju.whitemonday.common.util.JwtUtil;
import com.minju.whitemonday.product.controller.ProductController;
import com.minju.whitemonday.product.dto.ProductRequestDto;
import com.minju.whitemonday.product.dto.ProductResponseDto;
import com.minju.whitemonday.product.service.ProductService;
import com.minju.whitemonday.product.entity.Product;
import com.minju.whitemonday.product.repository.ProductRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ProductController.class)
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private JwtUtil jwtUtil;  // JWT 유틸리티 mock

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService, productRepository))
                .build();
    }

    private String generateJwtToken() {
        // 테스트용 임시 토큰 생성
        // 실제로는 로그인 과정 등을 통해 생성된 JWT를 사용합니다.
        return "Bearer " + "test-jwt-token";
    }

    @Test
    public void testGetAllProducts() throws Exception {
        // Mock data for product response
        ProductResponseDto responseDto = new ProductResponseDto(
                1L, "Test Product", "Test Description", 100, 50, false, null, null, null
        );

        // 상품 목록 반환 mock
        given(productService.getAllProducts()).willReturn(List.of(responseDto));

        // GET 요청 테스트
        mockMvc.perform(get("/api/products")
                        .header("Authorization", generateJwtToken()))  // JWT 토큰 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Product"))
                .andExpect(jsonPath("$[0].price").value(100));
    }

    @Test
    public void testCreateProduct() throws Exception {
        // 상품 생성 요청에 대한 데이터 준비
        ProductRequestDto requestDto = new ProductRequestDto("New Product", "New Product Description", 150, 100, false, null);

        // 서비스 레이어에서 반환할 응답 DTO 생성
        ProductResponseDto responseDto = new ProductResponseDto(1L, "New Product", "New Product Description", 150, 100, false, null, null, null);

        // 상품 생성 시 호출되는 서비스 메서드 Mocking
        given(productService.addProduct(requestDto)).willReturn(responseDto);

        // POST 요청을 통해 상품 생성 테스트
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Product\",\"description\":\"New Product Description\",\"price\":150,\"stock\":100,\"flashSale\":false}")
                        .header("Authorization", generateJwtToken()))  // JWT 토큰 추가
                .andExpect(status().isCreated())  // 상태 코드 201 생성 완료
                .andExpect(jsonPath("$.title").value("New Product"))
                .andExpect(jsonPath("$.price").value(150));
    }
}
