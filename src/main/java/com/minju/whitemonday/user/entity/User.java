//package com.minju.whitemonday.user.entity;
//
//import com.minju.whitemonday.common.dto.UserRoleEnum;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Getter
//@Setter
//@NoArgsConstructor
//@Table(name = "user")
//public class User {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true)
//    private String username;
//
//    @Column(nullable = false)
//    private String password;
//
//    @Column(nullable = false, unique = true)
//    private String email;
//
//    @Column(nullable = false)
//    @Enumerated(value = EnumType.STRING)
//    private UserRoleEnum role;
//
//    @Column(nullable = true)
//    private Long lastPasswordUpdateTime = System.currentTimeMillis(); // 비밀번호 변경 시점 초기화
//
//    @Column(nullable = true)
//    private String address;
//
//    @Column(nullable = true)
//    private String name;
//
//    public User(String username, String password, String email, UserRoleEnum role) {
//        this.username = username;
//        this.password = password;
//        this.email = email;
//        this.role = role;
//    }
//
//    private boolean isEnabled = false; // 기본값 비활성화
//}
