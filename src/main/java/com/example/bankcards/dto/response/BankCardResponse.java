package com.example.bankcards.dto.response;

import com.example.bankcards.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BankCardResponse {
    private Long id;
    private String cardNumberMasked;
    private String cardHolder;
    private LocalDate expiryDate;
    private Card.CardStatus status;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}