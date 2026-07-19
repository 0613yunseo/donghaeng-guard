package com.nulla.donghaeguard.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String nickname;

    private String email;

    @Column(nullable = false)
    private String loginType;  // GUEST, EMAIL, SOCIAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum UserStatus {
        ACTIVE, INACTIVE
    }
}