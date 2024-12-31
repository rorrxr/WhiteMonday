//package com.minju.whitemonday.product.entity;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Table(name = "product")
//public class Product {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String title;
//    private String description;
//    private int price;
//    private int stock;
//    private boolean isFlashSale;
//    private LocalDateTime flashSaleStartTime;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;
//
//    public Product(String title, String description, int price, int stock, boolean isFlashSale, LocalDateTime flashSaleStartTime) {
//        this.title = title;
//        this.description = description;
//        this.price = price;
//        this.stock = stock;
//        this.isFlashSale = isFlashSale;
//        this.flashSaleStartTime = flashSaleStartTime;
//    }
//
//
//}
