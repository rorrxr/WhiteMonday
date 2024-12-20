package com.minju.whitemonday.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoDto {
    private String username;
    private boolean isAdmin;
}