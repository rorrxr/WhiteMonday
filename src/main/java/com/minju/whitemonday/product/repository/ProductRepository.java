package com.minju.whitemonday.product.repository;

import com.minju.whitemonday.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
