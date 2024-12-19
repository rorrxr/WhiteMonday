package com.minju.whitemonday.repository;

import com.minju.whitemonday.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
