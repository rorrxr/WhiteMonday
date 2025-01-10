package com.minju.wishlist.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

@Component
public class UserFeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 404:
                return new IllegalArgumentException("Post not found for the given ID");
            case 400:
                return new IllegalArgumentException("Bad request while calling Post Service");
            case 500:
                return new RuntimeException("에러났음요");
            default:
                return new RuntimeException("Unexpected error occurred: " + response.status());
        }
    }
}