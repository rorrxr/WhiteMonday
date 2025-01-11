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

        given(productService.getAllProducts()).willReturn(List.of(responseDto));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", generateJwtToken()))  // JWT 토큰 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Test Product"))
                .andExpect(jsonPath("$[0].price").value(100));
    }

    @Test
    public void testCreateProduct() throws Exception {
        // Test data for product creation
        ProductRequestDto requestDto = new ProductRequestDto("New Product", "New Product Description", 150, 100, false, null);
        ProductResponseDto responseDto = new ProductResponseDto(1L, "New Product", "New Product Description", 150, 100, false, null, null, null);

        given(productService.addProduct(requestDto)).willReturn(responseDto);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Product\",\"description\":\"New Product Description\",\"price\":150,\"stock\":100,\"flashSale\":false}")
                        .header("Authorization", generateJwtToken()))  // JWT 토큰 추가
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Product"))
                .andExpect(jsonPath("$.price").value(150));
    }
}
