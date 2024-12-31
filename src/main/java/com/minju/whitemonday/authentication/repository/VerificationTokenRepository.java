//package com.minju.whitemonday.authentication.repository;
//
//import com.minju.whitemonday.authentication.entity.VerificationToken;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.Optional;
//
//public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
//    Optional<VerificationToken> findByToken(String token);
//    Optional<VerificationToken> findByEmail(String email);
//}