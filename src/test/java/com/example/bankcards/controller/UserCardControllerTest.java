package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.AuthenticationService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        when(authenticationService.getCurrentUserId()).thenReturn(1L);
    }

    @Test
    void getUserCards_Success() throws Exception {
        Long userId = 1L;

        BankCardResponse card1 = BankCardResponse.builder()
                .id(1L)
                .cardNumberMasked("123456******3456")
                .balance(BigDecimal.valueOf(1000))
                .status(Card.CardStatus.ACTIVE)
                .build();

        BankCardResponse card2 = BankCardResponse.builder()
                .id(2L)
                .cardNumberMasked("987654******7654")
                .balance(BigDecimal.valueOf(500))
                .status(Card.CardStatus.ACTIVE)
                .build();

        Page<BankCardResponse> page = new PageImpl<>(List.of(card1, card2));

        when(cardService.getUserCards(eq(userId), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/user/cards")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].cardNumberMasked").value("123456******3456"))
                .andExpect(jsonPath("$.content[1].balance").value(500));
    }

    @Test
    void getTotalBalance_Success() throws Exception {
        Long userId = 1L;
        when(cardService.getTotalBalance(userId)).thenReturn(BigDecimal.valueOf(1500.50));

        mockMvc.perform(get("/api/user/cards/balance/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.5"));
    }

    @Test
    void transferBetweenCards_Success() throws Exception {
        Long userId = 1L;
        CardTransferRequest request = new CardTransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(100));

        doNothing().when(cardService).transferBetweenCards(any(CardTransferRequest.class), eq(userId));

        mockMvc.perform(post("/api/user/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cardService, times(1)).transferBetweenCards(any(), eq(userId));
    }

    @Test
    void requestBlockCard_Success() throws Exception {
        Long userId = 1L;
        Long cardId = 1L;

        doNothing().when(cardService).requestBlockCard(cardId, userId);

        mockMvc.perform(post("/api/user/cards/{cardId}/block", cardId))
                .andExpect(status().isAccepted());

        verify(cardService, times(1)).requestBlockCard(cardId, userId);
    }

    @Test
    void activateCard_Success() throws Exception {
        Long userId = 1L;
        Long cardId = 1L;

        doNothing().when(cardService).activateCard(cardId, userId);

        mockMvc.perform(post("/api/user/cards/{cardId}/activate", cardId))
                .andExpect(status().isOk());

        verify(cardService, times(1)).activateCard(cardId, userId);
    }
}