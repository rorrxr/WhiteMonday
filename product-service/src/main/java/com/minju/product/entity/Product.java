package com.minju.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


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
    private boolean flashSale;
    private String  flashSaleStartTime;

    @CreationTimestamp
    private String  createdAt;

    @UpdateTimestamp
    private String  updatedAt;

    public Product(Long id, String title, String description, int price, int stock, boolean flashSale, String  flashSaleStartTime) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.flashSale = flashSale;
        this.flashSaleStartTime = flashSaleStartTime;
    }
}
