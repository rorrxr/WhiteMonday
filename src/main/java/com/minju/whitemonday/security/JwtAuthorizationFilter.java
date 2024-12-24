package com.minju.whitemonday.security;

import com.minju.whitemonday.jwt.JwtUtil;
import com.minju.whitemonday.repository.UserRepository;
import com.minju.whitemonday.service.LogoutService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j(topic = "JWT 검증 및 인가")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

//    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService, LogoutService logoutService, UserRepository userRepository) {
//        this.jwtUtil = jwtUtil;
//        this.userDetailsService = userDetailsService;
//        this.logoutService = logoutService;
//        this.userRepository = userRepository;
//    }
//    @Override
//    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
//        String token = jwtUtil.getJwtFromHeader(req);
//
//        if (StringUtils.hasText(token)) {
//            log.info("Extracted token: {}", token);
//
//            if (!jwtUtil.validateToken(token)) {
//                log.error("Invalid JWT token");
//                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                res.getWriter().write("Invalid JWT");
//                return;
//            }
//
//            // 블랙리스트 확인
//            if (logoutService.isTokenBlacklisted(token)) {
//                log.error("Token is blacklisted: {}", token);
//                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//                res.getWriter().write("Token is blacklisted");
//                return;
//            }
//
//            Claims claims = jwtUtil.getUserInfoFromToken(token);
//            String username = claims.getSubject();
//
//            log.info("Extracted username: {}", username);
//
//            if (username != null) {
//                setAuthentication(username); // 인증 정보 설정
//            }
//        }
//
//        filterChain.doFilter(req, res); // 필터 체인 진행
//    }
//
//    private void setAuthentication(String username) {
//        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//        Authentication authentication = new UsernamePasswordAuthenticationToken(
//                userDetails, null, userDetails.getAuthorities()
//        );
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        log.info("SecurityContext set with user: {}", username);
//    }

    public JwtAuthorizationFilter(JwtUtil jwtUtil, UserDetailsServiceImpl userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {
        String token = jwtUtil.getJwtFromHeader(req);
        log.info("Token from header: {}", token);

        if (StringUtils.hasText(token)) {
            if (!jwtUtil.validateToken(token)) {
                log.error("Invalid JWT token");
                filterChain.doFilter(req, res);
                return;
            }

            Claims claims = jwtUtil.getUserInfoFromToken(token);
            log.info("Extracted username: {}", claims.getSubject());

            try {
                setAuthentication(claims.getSubject());
            } catch (Exception e) {
                log.error("Failed to set authentication: {}", e.getMessage());
            }
        } else {
            log.warn("No JWT token found in the request header");
        }

        filterChain.doFilter(req, res);
    }


    // 인증 처리
    public void setAuthentication(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(username);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}