package com.minju.wishlist;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;

@EnableFeignClients(basePackages = "com.minju.wishlist.client")
@SpringBootApplication
public class WishlistApplication implements CommandLineRunner {

	private final ApplicationContext context;

	public WishlistApplication(ApplicationContext context) {
		this.context = context;
	}
	@Override
	public void run(String... args) {
		// ProductServiceClient Bean 확인
		if (context.containsBean("productServiceClient")) {
			System.out.println("잘됨");
		} else {
			System.out.println("ㅅㅂ");
		}
	}
	public static void main(String[] args) {
		SpringApplication.run(WishlistApplication.class, args);
	}


}