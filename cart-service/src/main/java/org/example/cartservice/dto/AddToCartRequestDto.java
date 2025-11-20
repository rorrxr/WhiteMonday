package org.example.cartservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AddToCartRequestDto {

    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private Integer quantity;
}