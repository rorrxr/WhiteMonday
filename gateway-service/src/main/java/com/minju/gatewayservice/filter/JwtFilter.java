package com.minju.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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

@Component
@Slf4j
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret-key}")
    private String secret;


    @Autowired
    private ObjectMapper objectMapper;

    private SecretKey secretKey;

    public JwtFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (secret == null || secret.isEmpty()) {
            log.error("JWT_SECRET_KEY is not set!");
        } else {
            log.info("JWT_SECRET_KEY loaded successfully");
        }
        byte[] bytes = Base64.getDecoder().decode(secret);
        secretKey = Keys.hmacShaKeyFor(bytes);  // Base64 디코딩하여 SecretKey 생성
        log.debug("Initialized secret key: {}", secretKey);
    }

    @Override
    public GatewayFilter apply(Config config) {
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

    private String getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.get("userId", String.class);
    }

    private String getRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("auth", String.class);
    }

    private Boolean isExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey) // SecretKeySpec으로 생성된 secretKey 사용
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expirationDate = claims.getExpiration(); // 토큰 만료일 가져오기
            return expirationDate.before(new Date()); // 만료 여부 확인
        } catch (JwtException e) {
            log.error("Error parsing token for expiration check: {}", e.getMessage());
            throw new RuntimeException("Invalid token", e); // 필요한 경우 적절한 예외 처리
        }
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
