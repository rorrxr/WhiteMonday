package com.minju.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequestDto {
    private String title;
    private String description;
    private int price;
    private int stock;
    private boolean isFlashSale;
    private String  flashSaleStartTime;
}
