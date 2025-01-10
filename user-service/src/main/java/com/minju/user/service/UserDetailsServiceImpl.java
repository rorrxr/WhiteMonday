package com.minju.user.service;

import com.minju.user.dto.SignupRequestDto;
import com.minju.user.dto.UserInfoDto;
import com.minju.user.dto.UserRoleEnum;
import com.minju.user.entity.User;
import com.minju.user.repository.UserRepository;
import com.minju.user.util.EncryptionUtil;
import com.minju.user.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Loading user by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new UsernameNotFoundException("Not Found " + username);
                });

        log.info("User found: {}", user.getUsername());
        log.info("Role: {}", user.getRole());

        return new UserDetailsImpl(user);
    }
}