package com.example.bankcards.util;

import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.LoginRequest;
import com.example.bankcards.dto.request.RegisterRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataBuilder {

    public static User.UserBuilder user() {
        return User.builder()
                .username("testuser")
                .email("test@bank.com")
                .password("password")
                .role(User.Role.USER)
                .enabled(true);
    }

    public static RegisterRequest.RegisterRequestBuilder registerRequest() {
        return RegisterRequest.builder()
                .username("newuser")
                .email("new@bank.com")
                .password("Password123!")
                .role(User.Role.USER);
    }

    public static LoginRequest.LoginRequestBuilder loginRequest() {
        return LoginRequest.builder()
                .username("testuser")
                .password("Password123!");
    }

    public static CreateCardRequest.CreateCardRequestBuilder createCardRequest() {
        return CreateCardRequest.builder()
                .cardHolder("Test User")
                .userId(1L);
    }

    public static Card.CardBuilder bankCard() {
        return Card.builder()
                .cardNumberMasked("**** **** **** 1234")
                .cardHolder("Test User")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    public static CardTransferRequest.CardTransferRequestBuilder cardTransferRequest() {
        return CardTransferRequest.builder()
                .fromCardId(1L)
                .toCardId(2L)
                .amount(new BigDecimal("100.00"));
    }
}