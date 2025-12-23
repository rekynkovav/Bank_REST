package com.example.bankcards.controller;

import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.dto.filter.CardFilter;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminCardController.class,
        excludeFilters = @ComponentScan.Filter(
                type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    void createCard_Success() throws Exception {
        // Arrange
        CreateCardRequest request = new CreateCardRequest();
        request.setUserId(1L);
        request.setCardHolder("John Doe");

        // Можно использовать упрощенный объект User
        User user = User.builder()
                .id(1L)
                .username("johndoe")
                .role(User.Role.USER) // Только обязательные поля
                .build();

        Card card = Card.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted_card_number")
                .cardNumberMasked("123456******3456")
                .cardHolder("John Doe")
                .expiryDate(LocalDate.now().plusYears(3))
                .cvvEncrypted("encrypted_cvv")
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.createCard(any(CreateCardRequest.class), anyLong())).thenReturn(card);

        // Act & Assert
        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumberMasked").value("123456******3456"));
    }


    @Test
    void getAllCards_Success() throws Exception {
        // Arrange
        BankCardResponse card1 = BankCardResponse.builder()
                .id(1L)
                .cardNumberMasked("1234567890123456")
                .balance(BigDecimal.valueOf(1000))
                .status(Card.CardStatus.ACTIVE)
                .build();

        BankCardResponse card2 = BankCardResponse.builder()
                .id(2L)
                .cardNumberMasked("9876543210987654")
                .balance(BigDecimal.valueOf(500))
                .status(Card.CardStatus.BLOCKED)
                .build();

        Page<BankCardResponse> page = new PageImpl<>(List.of(card1, card2));

        when(cardService.getAllCardsWithFilter(any(CardFilter.class), any())).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/cards")
                        .param("status", "ACTIVE")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    @Test
    void blockCard_Success() throws Exception {
        // Arrange
        doNothing().when(cardService).blockCardByAdmin(anyLong());

        // Act & Assert
        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService, times(1)).blockCardByAdmin(1L);
    }

    @Test
    void activateCard_Success() throws Exception {
        // Arrange
        doNothing().when(cardService).activateCardByAdmin(anyLong());

        // Act & Assert
        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isOk());

        verify(cardService, times(1)).activateCardByAdmin(1L);
    }

    @Test
    void deleteCard_Success() throws Exception {
        // Arrange
        doNothing().when(cardService).deleteCard(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).deleteCard(1L);
    }

    @Test
    void createCard_ValidationFailed() throws Exception {
        // Arrange
        CreateCardRequest request = new CreateCardRequest();
        // Не заполнены обязательные поля

        // Act & Assert
        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}