package com.example.bankcards.controller;

import com.example.bankcards.dto.filter.CardFilter;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.entity.Card;
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

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@Tag(name = "Admin Cards", description = "Управление картами администратором")
@SecurityRequirement(name = "bearerAuth")
public class AdminCardController {

    private final CardService cardService;

    @PostMapping
    @Operation(summary = "Создать новую карту для пользователя")
    public ResponseEntity<Card> createCard(@Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.createCard(request, request.getUserId());
        return ResponseEntity.ok(card);
    }

    @GetMapping
    @Operation(summary = "Получить все карты (администратор)")
    public ResponseEntity<Page<BankCardResponse>> getAllCards(
            @ParameterObject @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @RequestParam(required = false) Card.CardStatus status,
            @RequestParam(required = false) Long userId) {

        CardFilter filter = new CardFilter();
        filter.setStatus(status);
        filter.setUserId(userId);

        Page<BankCardResponse> cards = cardService.getAllCardsWithFilter(filter, pageable);
        return ResponseEntity.ok(cards);
    }

    @PutMapping("/{cardId}/block")
    @Operation(summary = "Заблокировать карту")
    public ResponseEntity<Void> blockCard(@PathVariable Long cardId) {
        cardService.blockCardByAdmin(cardId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cardId}/activate")
    @Operation(summary = "Активировать карту")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        cardService.activateCardByAdmin(cardId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{cardId}")
    @Operation(summary = "Удалить карту")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }
}