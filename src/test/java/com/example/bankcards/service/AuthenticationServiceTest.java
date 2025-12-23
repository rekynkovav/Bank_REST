package com.example.bankcards.service;

import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.dto.response.AuthResponse;
import com.example.bankcards.entity.CustomUserDetails;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@bank.com");
        request.setPassword("password123");
        request.setRole(User.Role.USER);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@bank.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(jwtService.getExpirationTime()).thenReturn(86400000L);

        var response = authenticationService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals(1L, response.getUserId());
        assertEquals("newuser", response.getUsername());
        verify(userRepository, times(1)).save(any());
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void register_UsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () ->
                authenticationService.register(request));
    }

    @Test
    void login_Success() {
        String username = "testuser";
        String password = "password";
        String email = "test@example.com";
        Long userId = 1L;
        String expectedToken = "jwt-token";
        Long expectedExpiration = 86400000L;

        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        CustomUserDetails mockUserDetails = mock(CustomUserDetails.class);
        when(mockUserDetails.getId()).thenReturn(userId);
        when(mockUserDetails.getUsername()).thenReturn(username);
        when(mockUserDetails.getEmail()).thenReturn(email);

        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        when(mockUserDetails.getAuthorities()).thenReturn(authorities);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        when(jwtService.generateToken(mockUserDetails)).thenReturn(expectedToken);
        when(jwtService.getExpirationTime()).thenReturn(expectedExpiration);

        AuthResponse response = authenticationService.login(request);

        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());
        assertEquals(userId, response.getUserId());
        assertEquals(username, response.getUsername());
        assertEquals(email, response.getEmail());
        assertEquals("USER", response.getRole());
        assertEquals(expectedExpiration, response.getExpiresIn());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(mockUserDetails);
        verify(mockAuth, times(1)).getPrincipal();

        verify(auditService, times(1)).logAction(
                eq(AuditService.Actions.USER_LOGIN),
                eq(AuditService.EntityTypes.USER),
                eq(userId),
                contains(username)
        );
    }
}