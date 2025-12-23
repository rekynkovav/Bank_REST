package com.example.bankcards.controller;

import com.example.bankcards.config.JwtAuthenticationFilter;
import com.example.bankcards.dto.filter.CardFilter;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.YearMonth;
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

    @BeforeEach
    void setup() {
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void createCard_Success() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .userId(1L)
                .cardHolder("JOHN DOE")
                .cardNumber("4111111111111111")
                .expiryDate(YearMonth.now().plusYears(3))
                .cvv("123")
                .balance(BigDecimal.valueOf(1000.00))
                .build();

        User user = User.builder()
                .id(1L)
                .username("johndoe")
                .role(User.Role.USER)
                .build();

        LocalDate expiryLocalDate = request.getExpiryDate().atDay(1);

        Card card = Card.builder()
                .id(1L)
                .cardNumberEncrypted("encrypted_card_number")
                .cardNumberMasked("411111******1111")
                .cardHolder("JOHN DOE")
                .expiryDate(expiryLocalDate)
                .cvvEncrypted("encrypted_cvv")
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000.00))
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.createCard(any(CreateCardRequest.class), anyLong()))
                .thenReturn(card);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardNumberMasked").value("411111******1111"))
                .andExpect(jsonPath("$.cardHolder").value("JOHN DOE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createCard_WithOnlyRequiredFields_Success() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .userId(1L)
                .cardHolder("JANE DOE")
                .cardNumber("5500000000000004")
                .expiryDate(YearMonth.now().plusYears(2))
                .cvv("456")
                .build();

        User user = User.builder()
                .id(2L)
                .username("janedoe")
                .role(User.Role.USER)
                .build();

        LocalDate expiryLocalDate = request.getExpiryDate().atDay(1);

        Card card = Card.builder()
                .id(2L)
                .cardNumberEncrypted("encrypted_card_number_2")
                .cardNumberMasked("550000******0004")
                .cardHolder("JANE DOE")
                .expiryDate(expiryLocalDate)
                .cvvEncrypted("encrypted_cvv_2")
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.createCard(any(CreateCardRequest.class), eq(1L)))
                .thenReturn(card);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(cardService, times(1)).createCard(any(CreateCardRequest.class), eq(1L));
    }

    @Test
    void createCard_ValidationFailed_MissingRequiredFields() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .cardHolder("JOHN DOE")
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.userId").value("User ID is required")) // Добавляем проверку userId
                .andExpect(jsonPath("$.cardNumber").value("Card number is required"))
                .andExpect(jsonPath("$.expiryDate").value("Expiry date is required"))
                .andExpect(jsonPath("$.cvv").value("CVV is required"));
    }

    @Test
    void createCard_ValidationFailed_InvalidCardNumber() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .cardHolder("JOHN DOE")
                .cardNumber("411111111111111")
                .expiryDate(YearMonth.now().plusYears(2))
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cardNumber").value("Card number must be 16 digits"));
    }

    @Test
    void createCard_ValidationFailed_PastExpiryDate() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .cardHolder("JOHN DOE")
                .cardNumber("4111111111111111")
                .expiryDate(YearMonth.now().minusMonths(1))
                .cvv("123")
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.expiryDate").value("Card must not be expired"));
    }

    @Test
    void createCard_ValidationFailed_InvalidCVV() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .cardHolder("JOHN DOE")
                .cardNumber("4111111111111111")
                .expiryDate(YearMonth.now().plusYears(2))
                .cvv("12")
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cvv").value("CVV must be 3 or 4 digits"));
    }

    @Test
    void createCard_ValidationFailed_NegativeBalance() throws Exception {
        CreateCardRequest request = CreateCardRequest.builder()
                .cardHolder("JOHN DOE")
                .cardNumber("4111111111111111")
                .expiryDate(YearMonth.now().plusYears(2))
                .cvv("123")
                .balance(BigDecimal.valueOf(-100.00))
                .build();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.balance").value("Balance cannot be negative"));
    }

    @Test
    void getAllCards_Success() throws Exception {
        BankCardResponse card1 = BankCardResponse.builder()
                .id(1L)
                .cardNumberMasked("411111******1111")
                .cardHolder("JOHN DOE")
                .expiryDate(LocalDate.now().plusYears(2))
                .balance(BigDecimal.valueOf(1000))
                .status(Card.CardStatus.ACTIVE)
                .build();

        BankCardResponse card2 = BankCardResponse.builder()
                .id(2L)
                .cardNumberMasked("550000******0004")
                .cardHolder("JANE DOE")
                .expiryDate(LocalDate.now().plusYears(3))
                .balance(BigDecimal.valueOf(500))
                .status(Card.CardStatus.BLOCKED)
                .build();

        Page<BankCardResponse> page = new PageImpl<>(List.of(card1, card2));

        when(cardService.getAllCardsWithFilter(any(CardFilter.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/cards")
                        .param("status", "ACTIVE")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].cardHolder").value("JOHN DOE"));
    }

    @Test
    void blockCard_Success() throws Exception {
        doNothing().when(cardService).blockCardByAdmin(anyLong());

        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService, times(1)).blockCardByAdmin(1L);
    }

    @Test
    void activateCard_Success() throws Exception {
        doNothing().when(cardService).activateCardByAdmin(anyLong());

        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isOk());

        verify(cardService, times(1)).activateCardByAdmin(1L);
    }

    @Test
    void deleteCard_Success() throws Exception {
        doNothing().when(cardService).deleteCard(anyLong());

        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).deleteCard(1L);
    }

    @Test
    void createCard_ValidationFailed_EmptyRequest() throws Exception {
        CreateCardRequest request = new CreateCardRequest();

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_WithJsonFormat_Success() throws Exception {
        String jsonRequest = """
    {
        "userId": 1,
        "cardHolder": "JOHN SMITH",
        "cardNumber": "4111111111111111",
        "expiryDate": "12/26",
        "cvv": "123",
        "balance": 1500.00
    }
    """;

        User user = User.builder()
                .id(3L)
                .username("johnsmith")
                .role(User.Role.USER)
                .build();

        LocalDate expiryLocalDate = YearMonth.of(2026, 12).atDay(1);

        Card card = Card.builder()
                .id(3L)
                .cardNumberEncrypted("encrypted_card_number_3")
                .cardNumberMasked("411111******1111")
                .cardHolder("JOHN SMITH")
                .expiryDate(expiryLocalDate)
                .cvvEncrypted("encrypted_cvv_3")
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1500.00))
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardService.createCard(any(CreateCardRequest.class), eq(1L)))
                .thenReturn(card);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.cardHolder").value("JOHN SMITH"));
    }
}