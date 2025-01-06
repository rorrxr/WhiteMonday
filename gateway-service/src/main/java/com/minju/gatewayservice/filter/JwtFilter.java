package com.minju.gatewayservice.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.SignatureAlgorithm;

@Component
@Slf4j
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    @Autowired
    private ObjectMapper objectMapper;

    private SecretKey secretKey;

    // 생성자
    public JwtFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
        log.debug("Using secret key: {}", secretKey);

        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 인증을 건너뛰고 싶은 경로 체크 (여기서는 /api/products 경로를 제외)
            if (request.getURI().getPath().startsWith("/api/products") && request.getMethod().equals("POST")) {
                log.debug("Skipping JWT authentication for product registration.");
                return chain.filter(exchange);  // 상품 등록 POST 요청은 인증을 건너뛴다.
            }

            // Authorization 헤더 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.debug("No Authorization header found, passing request to next filter.");
                return chain.filter(exchange);  // Authorization 헤더가 없으면 바로 필터 체인 통과
            }

            String accessToken = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            if (!accessToken.startsWith("Bearer ")) {
                log.error("Invalid token format: {}", accessToken);
                return setResponse(response, "Invalid token format.");
            }

            accessToken = accessToken.substring(7); // Remove "Bearer " prefix

            // 토큰 만료 체크
            try {
                isExpired(accessToken);
            } catch (ExpiredJwtException e) {
                log.error("Expired token: {}", accessToken);
                return setResponse(response, "Expired token.");
            }

            // 역할(role) 확인
            String role = getRole(accessToken);
            if (!hasRequiredRole(config.requiredRole, role)) {
                log.error("Insufficient role: required {}, but got {}", config.requiredRole, role);
                return setResponse(response, "Insufficient role.");
            }

            String userId = String.valueOf(getUserId(accessToken));
            log.debug("User ID: {}", userId);

            // Add headers for userId and role
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
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

    // 역할 계층을 고려한 권한 확인
    private boolean hasRequiredRole(String requiredRole, String userRole) {
        if ("USER".equals(requiredRole) && "ADMIN".equals(userRole)) {
            return true; // ADMIN은 USER 권한을 포함하므로 접근 허용
        }
        return requiredRole.equals(userRole);
    }

    private Long getUserId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId", Long.class);
    }

    private String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    private Boolean isExpired(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .before(new Date());
    }

    private String getCategory(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("category", String.class);
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Config {
        private String requiredRole;
    }

    @Data
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL) // Null 값을 가진 필드는 직렬화에서 제외
    private static class ResponseDto<T> {

        private String message;
        private T data;

        public ResponseDto(String message) {
            this.message = message;
        }
    }
}