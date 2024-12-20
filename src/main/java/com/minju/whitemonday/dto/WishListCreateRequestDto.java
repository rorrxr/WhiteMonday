package com.minju.whitemonday.dto;

import lombok.*;

@Data
public class WishListCreateRequestDto {
    private Long productId;
    private int quantity;
}