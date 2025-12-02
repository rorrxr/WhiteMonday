package com.minju.user.controller;

import com.minju.common.dto.CommonResponse;
import com.minju.common.exception.BusinessException;
import com.minju.common.exception.ErrorCode;
import com.minju.user.dto.SignupRequestDto;
import com.minju.user.dto.UserInfoDto;
import com.minju.user.service.LogoutService;
import com.minju.user.service.UserService;
import com.minju.user.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final Environment env;
    private final UserService userService;
    private final LogoutService logoutService;
    private final JwtUtil jwtUtil;

    @GetMapping("/test")
    public ResponseEntity<CommonResponse<String>> status() {
        String msg = String.format("It's Working in User Service on PORT %s", env.getProperty("local.server.port"));
        return ResponseEntity.ok(CommonResponse.success("유저 서비스 상태 확인", msg));
    }

    /** 1. 회원가입 */
    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<Void>> signup(@Valid @RequestBody SignupRequestDto requestDto) {
        userService.signup(requestDto);
        return ResponseEntity.ok(
                CommonResponse.success("회원가입이 완료되었습니다.", null)
        );
    }

    /** 2. 로그인 */
    @PostMapping("/login")
    public ResponseEntity<CommonResponse<Map<String, String>>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 로그인 실패 시 userService.login() 안에서 BusinessException(ErrorCode.LOGIN_FAILED) 던지도록 설계해두면 깔끔
        Map<String, String> tokens = userService.login(username, password);

        return ResponseEntity.ok(
                CommonResponse.success("로그인에 성공했습니다.", tokens)
        );
    }

    /** 3. 로그아웃 */
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith(JwtUtil.BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "유효하지 않은 Authorization 헤더입니다.");
        }

        String refreshToken = authorizationHeader.substring(JwtUtil.BEARER_PREFIX.length());

        try {
            userService.logout(refreshToken);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(
                    CommonResponse.success("로그아웃에 성공했습니다.", null)
            );
        } catch (ExpiredJwtException e) {
            // 만료된 토큰도 블랙리스트 처리하려면 여기서도 로직 추가 가능
            log.warn("Expired token during logout", e);
            logoutService.invalidateToken(refreshToken);
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok(
                    CommonResponse.success("만료된 토큰이지만 로그아웃 처리되었습니다.", null)
            );
        } catch (Exception e) {
            log.error("Unexpected error during logout: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "로그아웃 처리 중 오류가 발생했습니다.");
        }
    }

    /** 4. 사용자 정보 조회 (X-User-Id 기반) */
    @GetMapping("/info")
    public ResponseEntity<CommonResponse<UserInfoDto>> getUserInfo(
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {

        if (userId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "사용자 인증 정보가 필요합니다.");
        }

        log.info("Fetching user info for userId: {}", userId);
        UserInfoDto userInfo = userService.getUserInfo(userId);

        return ResponseEntity.ok(
                CommonResponse.success("사용자 정보 조회에 성공했습니다.", userInfo)
        );
    }
}
