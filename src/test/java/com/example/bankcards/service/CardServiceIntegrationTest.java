package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CardServiceIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private BankCardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private EncryptionService encryptionService;

    private User testUser;
    private Card card1;
    private Card card2;

    @BeforeEach
    void setUp() {
        when(encryptionService.encrypt(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Возвращаем тот же текст

        when(encryptionService.decrypt(anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0)); // Возвращаем тот же текст

        when(encryptionService.maskCardNumber(anyString()))
                .thenAnswer(invocation -> {
                    String cardNumber = invocation.getArgument(0);
                    return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
                });

        cardRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("integration_test_user")
                .password("password")
                .email("integration@test.com")
                .role(User.Role.USER)
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        CreateCardRequest request1 = new CreateCardRequest();
        request1.setCardHolder("Integration Test User");
        request1.setUserId(testUser.getId());

        CreateCardRequest request2 = new CreateCardRequest();
        request2.setCardHolder("Integration Test User");
        request2.setUserId(testUser.getId());

        card1 = cardService.createCard(request1, testUser.getId());
        card2 = cardService.createCard(request2, testUser.getId());

        card1.setBalance(new BigDecimal("1000.00"));
        card2.setBalance(new BigDecimal("500.00"));
        cardRepository.save(card1);
        cardRepository.save(card2);
    }

    @Test
    void transferBetweenCards_IntegrationTest() {
        CardTransferRequest request = new CardTransferRequest();
        request.setFromCardId(card1.getId());
        request.setToCardId(card2.getId());
        request.setAmount(new BigDecimal("200.00"));

        cardService.transferBetweenCards(request, testUser.getId());

        var updatedCard1 = cardRepository.findById(card1.getId());
        var updatedCard2 = cardRepository.findById(card2.getId());

        assertTrue(updatedCard1.isPresent());
        assertTrue(updatedCard2.isPresent());

        assertEquals(new BigDecimal("800.00"), updatedCard1.get().getBalance());
        assertEquals(new BigDecimal("700.00"), updatedCard2.get().getBalance());
    }

    @Test
    void getTotalBalance_IntegrationTest() {
        BigDecimal totalBalance = cardService.getTotalBalance(testUser.getId());

        assertEquals(new BigDecimal("1500.00"), totalBalance);
    }

    @Test
    void blockCardByAdmin_IntegrationTest() {
        cardService.blockCardByAdmin(card1.getId());

        var blockedCard = cardRepository.findById(card1.getId());
        assertTrue(blockedCard.isPresent());
        assertEquals(Card.CardStatus.BLOCKED, blockedCard.get().getStatus());
    }
}