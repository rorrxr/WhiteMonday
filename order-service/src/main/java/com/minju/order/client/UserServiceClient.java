package com.minju.order.client;

import com.minju.common.dto.UserInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name = "user-service", url = "http://localhost:8081")
//public interface UserServiceClient {
//
//    @GetMapping("/api/user/{userId}")
//    UserInfoDto getUserInfo(@PathVariable Long userId);
//}
