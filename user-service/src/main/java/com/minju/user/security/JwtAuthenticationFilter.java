package com.minju.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minju.user.dto.LoginRequestDto;
import com.minju.user.dto.UserRoleEnum;
import com.minju.user.service.UserDetailsImpl;
import com.minju.user.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/user/login"); // 로그인 엔드포인트 설정
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            // 요청 데이터 읽기
            LoginRequestDto requestDto = new ObjectMapper().readValue(request.getInputStream(), LoginRequestDto.class);

            log.info("Attempting authentication for username: {}", requestDto.getUsername());

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            requestDto.getUsername(),
                            requestDto.getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error("Error reading login request: {}", e.getMessage());
            throw new RuntimeException("로그인 요청 처리 중 에러 발생");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException {
        UserDetailsImpl userDetails = (UserDetailsImpl) authResult.getPrincipal();
        String username = userDetails.getUsername();
        Long userId = userDetails.getUser().getId();
        UserRoleEnum role = userDetails.getUser().getRole();

        log.info("Authentication successful for username: {}", username);

        // 액세스 토큰과 리프레시 토큰 생성
        String accessToken = jwtUtil.createAccessToken(userId, username, role.getAuthority());
//        String refreshToken = jwtUtil.createRefreshToken(username);

        log.info("Generated Access Token: {}", accessToken);
//        log.info("Generated Refresh Token: {}", refreshToken);

        // 응답 JSON 생성
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
//        tokens.put("refreshToken", refreshToken);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        new ObjectMapper().writeValue(response.getWriter(), tokens);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        log.error("Authentication failed: {}", failed.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, String> error = new HashMap<>();
        error.put("error", "Authentication failed");
        error.put("message", failed.getMessage());

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            new ObjectMapper().writeValue(response.getWriter(), error);
        } catch (IOException e) {
            log.error("Error writing failure response: {}", e.getMessage());
        }
    }
}
