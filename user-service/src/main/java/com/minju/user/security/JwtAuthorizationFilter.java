package com.minju.user.security;

import com.minju.user.repository.UserRepository;
import com.minju.user.service.LogoutService;
import com.minju.user.service.UserDetailsServiceImpl;
import com.minju.user.util.JwtUtil;
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
//    private final LogoutService logoutService;
//    private final UserRepository userRepository;

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
//        if (token == null) {
//            log.warn("No Authorization header found in the request");
//        } else {
//            log.info("Authorization token: {}", token);
//        }
//
//        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
//            Claims claims = jwtUtil.getUserInfoFromToken(token);
//            String username = claims.getSubject();
//
//            if (username != null) {
//                setAuthentication(username);
//            }
//        }
//
//        filterChain.doFilter(req, res);
//    }

//    private void setAuthentication(String username) {
//        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//        Authentication authentication = new UsernamePasswordAuthenticationToken(
//                userDetails, null, userDetails.getAuthorities()
//        );
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//
//        // 로그 추가
//        log.info("SecurityContext set with user: {}", username);
//        log.info("Authentication in SecurityContext: {}", SecurityContextHolder.getContext().getAuthentication());
//    }




    // --------

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
        log.info("Received token: {}", token);
        log.info("Token is valid: {}", jwtUtil.validateToken(token));

        filterChain.doFilter(req, res);
    }


//    // 인증 처리
//    public void setAuthentication(String username) {
//        SecurityContext context = SecurityContextHolder.createEmptyContext();
//        Authentication authentication = createAuthentication(username);
//        context.setAuthentication(authentication);
//
//        SecurityContextHolder.setContext(context);
//    }

    private void setAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // 인증 객체 생성
    private Authentication createAuthentication(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }
}