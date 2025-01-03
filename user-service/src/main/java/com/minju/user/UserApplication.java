package com.minju.user;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class UserApplication {

	public static void main(String[] args) {

//		Dotenv dotenv = Dotenv.configure().load();
//		System.setProperty("JWT_SECRET_KEY", dotenv.get("JWT_SECRET_KEY"));
		SpringApplication.run(UserApplication.class, args);	}

}
