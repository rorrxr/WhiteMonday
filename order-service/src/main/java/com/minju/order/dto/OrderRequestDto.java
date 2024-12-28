package com.minju.order.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderRequestDto {
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private Long productId;
        private int quantity;
    }
}
