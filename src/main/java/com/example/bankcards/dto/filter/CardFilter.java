package com.example.bankcards.dto.filter;

import com.example.bankcards.entity.Card;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardFilter {
    private Card.CardStatus status;
    private BigDecimal minBalance;
    private BigDecimal maxBalance;
    private Long userId;
}