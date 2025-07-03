package com.minju.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {

    @Value("${jwt.secret-key}")
    private String secretKey; // JWT Secret Key를 주입받음

    private SecretKey key;

    private final ObjectMapper objectMapper; // ObjectMapper 주입

    public AuthorizationFilter(ObjectMapper objectMapper) { // 생성자에서 주입
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isEmpty()) {
            log.error("JWT_SECRET_KEY is not set!");
            throw new IllegalArgumentException("JWT_SECRET_KEY is required");
        }

        try {
            byte[] bytes = secretKey.getBytes(StandardCharsets.UTF_8);
            key = Keys.hmacShaKeyFor(bytes); // SecretKey 생성
            log.info("JWT_SECRET_KEY loaded and initialized successfully");
        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT_SECRET_KEY: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.debug("No Authorization header found, passing request to next filter.");
                return setResponse(response, "No authorization header");
            }

            String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                log.error("Invalid token format: {}", authorizationHeader);
                return setResponse(response, "Invalid token format.");
            }

            String token = authorizationHeader.substring(7); // "Bearer " 제거
            log.debug("Received Token: {}", token); // 추가된 로그

            try {
                isExpired(token);
            } catch (ExpiredJwtException e) {
                log.error("Expired token: {}", token);
                return setResponse(response, "Expired token.");
            } catch (MalformedJwtException | SignatureException e) {
                log.error("Invalid token: {}", token);
                return setResponse(response, "Invalid token.");
            }

//            String role = getRole(token);
//            log.debug("Parsed Role: {}", role); // 추가된 로그
//
//            if (!hasRequiredRole(config.requiredRole, role)) {
//                log.error("Insufficient role: required {}, but got {}", config.requiredRole, role);
//                return setResponse(response, "Insufficient role.");
//            }

            String userId = String.valueOf(getUserId(token));
            log.debug("Parsed User ID: {}", userId); // 추가된 로그

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }

    private Mono<Void> setResponse(ServerHttpResponse response, String data) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ResponseDto responseDto = new ResponseDto<>(data);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseDto);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            log.error("Error while writing response: {}", e.getMessage());
            byte[] errorBytes = "{\"message\":\"Server error occurred.\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
        }
    }

    private boolean hasRequiredRole(String requiredRole, String userRole) {
        return requiredRole.equals(userRole) || ("USER".equals(requiredRole) && "ADMIN".equals(userRole));
    }

    private Long getUserId(String token) {
        Claims claims = parseJwt(token);
        log.debug("Parsed Claims: {}", claims);
        Long userId = claims.get("userId", Long.class);
        return userId;
    }

//    private String getRole(String token) {
//        Claims claims = parseJwt(token);
//        log.debug("Parsed Claims: {}", claims); // 추가된 로그
//        log.debug("Role (auth): {}", claims.get("auth", String.class)); // 추가된 로그
//        return claims.get("auth", String.class);  // auth 필드 사용
//    }

    private void isExpired(String token) {
        Claims claims = parseJwt(token);
        log.debug("Expiration Date: {}", claims.getExpiration()); // 만료 시간 확인
        if (claims.getExpiration().before(new Date())) {
            throw new ExpiredJwtException(null, claims, "Token expired");
        }
    }

    private Claims parseJwt(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.debug("Parsed Claims: {}", claims);
            return claims;
        } catch (JwtException e) {
            log.error("JWT parsing failed: {}", e.getMessage());
            throw e;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Config {
        private String requiredRole;
    }

    @Data
    @NoArgsConstructor
    public static class ResponseDto<T> {
        private String message;
        private T data;

        public ResponseDto(String message) {
            this.message = message;
        }
    }
}

