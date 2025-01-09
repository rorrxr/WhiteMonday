package com.minju.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
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
import java.util.Date;

@Component
@Slf4j
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;  // JwtUtil에서 사용하는 secretKey와 동일해야 함

    @Autowired
    private ObjectMapper objectMapper;

    private SecretKey secretKey;

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

            // 토큰 검증
            try {
                isExpired(accessToken);
            } catch (ExpiredJwtException e) {
                log.error("Expired token: {}", accessToken);
                return setResponse(response, "Expired token.");
            } catch (MalformedJwtException | SignatureException e) {
                log.error("Invalid token: {}", accessToken);
                return setResponse(response, "Invalid token.");
            }

            // 역할 확인
            String role = getRole(accessToken);
            log.debug("Parsed role: {}", role);

            if (!hasRequiredRole(config.requiredRole, role)) {
                log.error("Insufficient role: required {}, but got {}", config.requiredRole, role);
                return setResponse(response, "Insufficient role.");
            }

            String userId = String.valueOf(getUserId(accessToken));
            log.debug("Parsed userId: {}", userId);

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

    private boolean hasRequiredRole(String requiredRole, String userRole) {
        return requiredRole.equals(userRole) || ("USER".equals(requiredRole) && "ADMIN".equals(userRole));
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

    @Data
    @NoArgsConstructor
    public static class Config {
        private String requiredRole;
    }

    @Data
    @NoArgsConstructor
    private static class ResponseDto<T> {
        private String message;
        private T data;
        public ResponseDto(String message) {
            this.message = message;
        }
    }
}
