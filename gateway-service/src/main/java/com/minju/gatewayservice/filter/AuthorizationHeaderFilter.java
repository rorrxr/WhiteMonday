package com.minju.gatewayservice.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@Slf4j
public class AuthorizationHeaderFilter extends AbstractGatewayFilterFactory<AuthorizationHeaderFilter.Config> {
    Environment env;

    public AuthorizationHeaderFilter(Environment env) {
        super(Config.class);
        this.env = env;
    }

    public static class Config {
        // Put configuration properties here
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().toString();

            // 경로 확인: /signup 및 /login 요청은 필터를 건너뜀
            if (path.contains("/signup") || path.contains("/login")) {
                return chain.filter(exchange);
            }
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "No authorization header", HttpStatus.UNAUTHORIZED);
            }

            HttpHeaders headers = request.getHeaders();
            Set<String> keys = headers.keySet();
            log.info(">>>");
            keys.stream().forEach(v -> {
                log.info(v + "=" + request.getHeaders().get(v));
            });
            log.info("<<<");

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
            String jwt = authorizationHeader.replace("Bearer", "");

            // Create a cookie object
//            ServerHttpResponse response = exchange.getResponse();
//            ResponseCookie c1 = ResponseCookie.from("my_token", "test1234").maxAge(60 * 60 * 24).build();
//            response.addCookie(c1);

            if (!isJwtValid(jwt)) {
                return onError(exchange, "JWT token is not valid", HttpStatus.UNAUTHORIZED);
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error(err);

        byte[] bytes = "The requested token is invalid.".getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return response.writeWith(Flux.just(buffer));

//        return response.setComplete();
    }

    private boolean isJwtValid(String jwt) {
        boolean returnValue = true;

        try {
            String subject = Jwts
                    .parser()
                    .setSigningKey(env.getProperty("token.secret"))
                    .parseClaimsJws(jwt)
                    .getBody()
                    .getSubject();

            if (subject == null || subject.isEmpty()) {
                returnValue = false;
            }
        } catch (ExpiredJwtException ex) {
            log.error("JWT token is expired");
            returnValue = false;
        } catch (Exception ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
            returnValue = false;
        }

        return returnValue;
    }

}