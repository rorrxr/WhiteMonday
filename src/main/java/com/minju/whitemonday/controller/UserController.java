package com.minju.whitemonday.controller;

import com.minju.whitemonday.dto.SignupRequestDto;
import com.minju.whitemonday.dto.UserInfoDto;
import com.minju.whitemonday.entity.UserRoleEnum;
import com.minju.whitemonday.security.UserDetailsImpl;
import com.minju.whitemonday.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/user-info")
    public ResponseEntity<UserInfoDto> join(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        // UserDetailsImpl에서 사용자 정보 가져오기
        String username = userDetails.getUsername();
        UserRoleEnum role = userDetails.getUser().getRole();
        boolean isAdmin = (role == UserRoleEnum.ADMIN);

        // UserInfoDto 객체 생성
        UserInfoDto userInfoDto = new UserInfoDto(username, isAdmin);

        // ResponseEntity로 반환
        return ResponseEntity.ok(userInfoDto);
    }

    // 로그인 데이터를 받아 인증을 수행합니다.
    @PostMapping("/api/user/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDto requestDto, BindingResult bindingResult) {
        // Validation 예외 처리
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError fieldError : bindingResult.getFieldErrors()) {
                errors.put(fieldError.getField(), fieldError.getDefaultMessage());
            }
            return ResponseEntity.badRequest().body(errors);
        }

        userService.signup(requestDto);
        return ResponseEntity.ok("Signup successful");
    }
}
