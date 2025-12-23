package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.dto.filter.CardFilter;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.AuthenticationService;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/user/cards")
@RequiredArgsConstructor
@Tag(name = "User Cards", description = "Управление картами пользователя")
@SecurityRequirement(name = "bearerAuth")
public class UserCardController {

    private final CardService cardService;
    private final AuthenticationService authService;

    @GetMapping
    @Operation(summary = "Получить список карт пользователя с фильтрацией")
    public ResponseEntity<Page<BankCardResponse>> getUserCards(
            @ParameterObject @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @ParameterObject CardFilter filter) {

        Long userId = authService.getCurrentUserId();
        Page<BankCardResponse> cards = cardService.getUserCards(userId, filter, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    @Operation(summary = "Получить информацию о конкретной карте")
    public ResponseEntity<BankCardResponse> getCard(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        Card card = cardService.getUserCardById(cardId, userId);
        BankCardResponse response = cardService.convertToResponse(card);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance/total")
    @Operation(summary = "Получить общий баланс по всем картам")
    public ResponseEntity<BigDecimal> getTotalBalance() {
        Long userId = authService.getCurrentUserId();
        BigDecimal totalBalance = cardService.getTotalBalance(userId);
        return ResponseEntity.ok(totalBalance);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между своими картами")
    public ResponseEntity<Void> transferBetweenCards(
            @Valid @RequestBody CardTransferRequest request) {

        Long userId = authService.getCurrentUserId();
        cardService.transferBetweenCards(request, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{cardId}/block")
    @Operation(summary = "Запрос на блокировку карты")
    public ResponseEntity<Void> requestBlockCard(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        cardService.requestBlockCard(cardId, userId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/{cardId}/activate")
    @Operation(summary = "Активировать карту")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        Long userId = authService.getCurrentUserId();
        cardService.activateCard(cardId, userId);
        return ResponseEntity.ok().build();
    }
}