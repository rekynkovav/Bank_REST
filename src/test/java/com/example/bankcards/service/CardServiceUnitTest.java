package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.entity.CardTransaction;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.encryption.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceUnitTest {

    @Mock
    private BankCardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardTransactionRepository transactionRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private CardService cardService;

    private User testUser;
    private Card testCard;
    private CardTransaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@bank.com")
                .role(User.Role.USER)
                .enabled(true)
                .build();

        testCard = Card.builder()
                .id(1L)
                .cardNumberMasked("**** **** **** 1234")
                .cardHolder("Test User")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(Card.CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .user(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testTransaction = CardTransaction.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .status(CardTransaction.TransactionStatus.SUCCESS)
                .build();
    }

    @Test
    void createCard_Success() {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardHolder("John Doe");
        request.setUserId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(encryptionService.encrypt(any())).thenReturn("encrypted");
        when(encryptionService.maskCardNumber(any())).thenReturn("**** **** **** 5678");
        when(cardRepository.save(any())).thenReturn(testCard);

        Card result = cardService.createCard(request, 1L);

        assertNotNull(result);
        assertEquals(testCard.getId(), result.getId());
        verify(userRepository, times(1)).findById(1L);
        verify(cardRepository, times(1)).save(any());
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void createCard_UserNotFound() {
        CreateCardRequest request = new CreateCardRequest();
        request.setUserId(999L);
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                cardService.createCard(request, 999L));
    }

    @Test
    void transferBetweenCards_Success() {
        CardTransferRequest request = new CardTransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        Card fromCard = Card.builder()
                .id(1L)
                .balance(new BigDecimal("1000.00"))
                .status(Card.CardStatus.ACTIVE)
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .balance(new BigDecimal("500.00"))
                .status(Card.CardStatus.ACTIVE)
                .user(testUser)
                .build();

        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any())).thenReturn(testTransaction);

        cardService.transferBetweenCards(request, 1L);

        verify(cardRepository, times(2)).save(any());
        verify(transactionRepository, times(1)).save(any());
        assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void transferBetweenCards_InsufficientFunds() {
        CardTransferRequest request = new CardTransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("1500.00"));

        Card fromCard = Card.builder()
                .id(1L)
                .balance(new BigDecimal("1000.00"))
                .status(Card.CardStatus.ACTIVE)
                .user(testUser)
                .build();

        Card toCard = Card.builder()
                .id(2L)
                .balance(new BigDecimal("500.00"))
                .status(Card.CardStatus.ACTIVE)
                .user(testUser)
                .build();

        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(toCard));

        assertThrows(InsufficientFundsException.class, () ->
                cardService.transferBetweenCards(request, 1L));

        verify(auditService, times(1)).logAction(any(), any());
    }

    @Test
    void transferBetweenCards_CardNotFound() {
        CardTransferRequest request = new CardTransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(new BigDecimal("100.00"));

        when(cardRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(com.example.bankcards.exception.CardNotFoundException.class, () ->
                cardService.transferBetweenCards(request, 1L));
    }

    @Test
    void blockCardByAdmin_Success() {
        Card activeCard = Card.builder()
                .id(1L)
                .status(Card.CardStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard));

        cardService.blockCardByAdmin(1L);

        assertEquals(Card.CardStatus.BLOCKED, activeCard.getStatus());
        assertNotNull(activeCard.getUpdatedAt());
        verify(cardRepository, times(1)).save(activeCard);
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void blockCardByAdmin_CardAlreadyBlocked() {
        Card blockedCard = Card.builder()
                .id(1L)
                .status(Card.CardStatus.BLOCKED)
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(blockedCard));

        cardService.blockCardByAdmin(1L);

        assertEquals(Card.CardStatus.BLOCKED, blockedCard.getStatus());
        verify(cardRepository, never()).save(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void activateCardByAdmin_Success() {
        Card blockedCard = Card.builder()
                .id(1L)
                .status(Card.CardStatus.BLOCKED)
                .expiryDate(LocalDate.now().plusYears(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(blockedCard));

        cardService.activateCardByAdmin(1L);

        assertEquals(Card.CardStatus.ACTIVE, blockedCard.getStatus());
        assertNotNull(blockedCard.getUpdatedAt());
        verify(cardRepository, times(1)).save(blockedCard);
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void getTotalBalance_Success() {
        when(cardRepository.getTotalBalanceByUserId(1L))
                .thenReturn(new BigDecimal("1500.00"));

        BigDecimal result = cardService.getTotalBalance(1L);

        assertEquals(new BigDecimal("1500.00"), result);
        verify(auditService, times(1)).logAction(any(), any());
    }

    @Test
    void getTotalBalance_ZeroBalance() {
        when(cardRepository.getTotalBalanceByUserId(1L))
                .thenReturn(null);

        BigDecimal result = cardService.getTotalBalance(1L);

        assertEquals(BigDecimal.ZERO, result);
        verify(auditService, times(1)).logAction(any(), any());
    }

    @Test
    void deleteCard_Success() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard));

        cardService.deleteCard(1L);

        verify(cardRepository, times(1)).deleteById(1L);
        verify(auditService, times(1)).logAction(any(), any(), any(), any());
    }

    @Test
    void deleteCard_CardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(com.example.bankcards.exception.CardNotFoundException.class, () ->
                cardService.deleteCard(1L));

        verify(cardRepository, never()).deleteById(any());
        verify(auditService, never()).logAction(any(), any(), any(), any());
    }

    @Test
    void generateCardNumber_ValidFormat() {
        String cardNumber = cardService.generateCardNumber();

        assertNotNull(cardNumber);
        assertEquals(16, cardNumber.length());
        assertTrue(cardNumber.matches("\\d{16}"));
    }

    @Test
    void generateCVV_ValidFormat() {
        String cvv = cardService.generateCVV();

        assertNotNull(cvv);
        assertEquals(3, cvv.length());
        assertTrue(cvv.matches("\\d{3}"));
        int cvvValue = Integer.parseInt(cvv);
        assertTrue(cvvValue >= 0 && cvvValue <= 999);
    }
}