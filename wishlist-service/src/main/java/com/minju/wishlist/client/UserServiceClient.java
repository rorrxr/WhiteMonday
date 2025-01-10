package com.minju.wishlist.client;

import com.minju.common.dto.UserInfoDto;
import com.minju.wishlist.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/user/{userId}")
    UserInfoDto getUserInfo(@PathVariable("userId") Long userId);
}
