package com.minju.user.controller;

import com.minju.user.entity.VerificationToken;
import com.minju.user.service.VerificationTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/verification")
@RequiredArgsConstructor
public class VerificationController {
    private final VerificationTokenService tokenService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateToken(@RequestParam String email) {
        VerificationToken token = tokenService.createVerificationToken(email);
        return ResponseEntity.ok("Verification token generated: " + token.getToken());
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestParam String token) {
        Optional<VerificationToken> validToken = tokenService.validateToken(token);
        if (validToken.isPresent()) {
            tokenService.markTokenAsVerified(token);
            return ResponseEntity.ok("Token validated successfully");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired token");
        }
    }
}
