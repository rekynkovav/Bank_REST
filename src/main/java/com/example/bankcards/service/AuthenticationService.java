package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.entity.CustomUserDetails;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService; // Добавлен AuditService

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(request.getRole())
                .enabled(true)
                .build();

        userRepository.save(user);

        String jwtToken = jwtService.generateToken(
                new CustomUserDetails(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getEmail(),
                        user.getAuthorities()
                )
        );

        // Аудит: регистрация пользователя
        auditService.logAction(
                AuditService.Actions.USER_REGISTERED,
                AuditService.EntityTypes.USER,
                user.getId(),
                String.format("New user registered: %s, role: %s",
                        request.getUsername(), request.getRole())
        );

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        String jwtToken = jwtService.generateToken(userDetails);

        // Аудит: вход пользователя
        auditService.logAction(
                AuditService.Actions.USER_LOGIN,
                AuditService.EntityTypes.USER,
                userDetails.getId(),
                String.format("User logged in: %s", request.getUsername())
        );

        return AuthResponse.builder()
                .token(jwtToken)
                .userId(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(userDetails.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .expiresIn(jwtService.getExpirationTime())
                .build();
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getId();
        }
        throw new IllegalStateException("User not authenticated");
    }
}