package com.example.bankcards.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String email;
    private String role;

    @Builder.Default
    private Long expiresIn = 86400000L;

    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();
}