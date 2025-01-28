package com.minju.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private int price;
    private int stock;
    private boolean isFlashSale;
    private LocalTime flashSaleStartTime;

    @CreationTimestamp
    private LocalTime createdAt;

    @UpdateTimestamp
    private LocalTime updatedAt;

    public Product(String title, String description, int price, int stock, boolean isFlashSale, LocalTime flashSaleStartTime) {
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.isFlashSale = isFlashSale;
        this.flashSaleStartTime = flashSaleStartTime;
    }


}
