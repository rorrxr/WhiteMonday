package com.minju.wishlist;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.minju.wishlist.client")
public class WishlistApplication {

	public static void main(String[] args) {

		SpringApplication.run(WishlistApplication.class, args);	}

}
