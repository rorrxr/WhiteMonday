package com.minju.gatewayservice.filter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.SignatureAlgorithm;

@Component
@Slf4j(topic = "[JwtFilter]")
public class JwtFilter extends AbstractGatewayFilterFactory<JwtFilter.Config> {

    @Value("${jwt.secret}") // Base64 Encode 한 SecretKey
    private String secret;
    private ObjectMapper objectMapper;

    private SecretKey secretKey;

    // Config 클래스에 requiredRole 추가
    public static class Config {
        private String requiredRole;  // 추가된 필드
        // Getter and Setter
        public String getRequiredRole() {
            return requiredRole;
        }

        public void setRequiredRole(String requiredRole) {
            this.requiredRole = requiredRole;
        }
    }

    public JwtFilter(ObjectMapper objectMapper){
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());

        return (((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Header에 AUTHORIZATION이 없으면
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return chain.filter(exchange); // Authorization 헤더가 없으면 필터 체인 계속 진행
            }

            String accessToken = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            if (!accessToken.startsWith("Bearer ")) {
                log.error("AccessToken 형식이 잘못됨 = {}", accessToken);
                return setResponse(response, "잘못된 형식의 AccessToken 입니다.");
            }

            String[] split = accessToken.split(" ");
            if (split.length < 2) {
                log.error("AccessToken 형식이 잘못됨 = {}", accessToken);
                return setResponse(response, "잘못된 형식의 AccessToken 입니다.");
            }

            accessToken = split[1];

            // 토큰 만료 여부 확인
            try {
                isExpired(accessToken);
            } catch (ExpiredJwtException e) {
                log.error("만료된 AccessToken = {}", accessToken);
                return setResponse(response, "만료된 AccessToken 입니다.");
            }

            // 토큰이 "access"인지 확인
            String category = getCategory(accessToken);
            if (!category.equals("access")) {
                log.error("AccessToken 이 아님 = {}", accessToken);
                return setResponse(response, "AccessToken 이 아닙니다.");
            }

            // 역할(role) 검증
            String role = getRole(accessToken);
            if (!hasRequiredRole(config.requiredRole, role)) {
                log.error("접근 권한이 없습니다. 필요 권한: {}, 사용자 권한: {}", config.requiredRole, role);
                return setResponse(response, "접근 권한이 없습니다.");
            }

            String userId = String.valueOf(getUserId(accessToken));
            // 요청 헤더에 userId, role 추가
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }));
    }

    // 역할 계층을 고려한 권한 확인
    private boolean hasRequiredRole(String requiredRole, String userRole) {
        // "USER"는 "ADMIN"에게 포함되므로, ADMIN은 USER를 대신할 수 있도록 함
        if ("USER".equals(requiredRole) && "ADMIN".equals(userRole)) {
            return true; // ADMIN은 USER 권한을 포함하므로 접근 허용
        }
        return requiredRole.equals(userRole); // "USER"와 "USER", "ADMIN"과 "ADMIN"의 비교
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

    private Mono<Void> setResponse(ServerHttpResponse response, String data) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        ResponseDto responseDto = new ResponseDto<>(data);
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(responseDto);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            log.error("응답 생성 중 오류 발생: {}", e.getMessage());
            byte[] errorBytes = "{\"message\":\"서버 오류가 발생했습니다.\"}".getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(errorBytes)));
        }
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
