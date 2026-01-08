package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.request.CardTransferRequest;
import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.dto.filter.CardFilter;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardTransaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.CardTransactionRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.specification.BankCardSpecification;
import com.example.bankcards.service.encryption.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final BankCardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardTransactionRepository transactionRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    private static final Random random = new Random();

    @Transactional(readOnly = true)
    public Page<BankCardResponse> getUserCards(Long userId, CardFilter filter, Pageable pageable) {
        Specification<Card> spec = Specification.where(BankCardSpecification.byUserId(userId));

        if (filter.getStatus() != null) {
            spec = spec.and(BankCardSpecification.byStatus(filter.getStatus()));
        }

        if (filter.getMinBalance() != null) {
            spec = spec.and(BankCardSpecification.balanceGreaterThanOrEqual(filter.getMinBalance()));
        }

        if (filter.getMaxBalance() != null) {
            spec = spec.and(BankCardSpecification.balanceLessThanOrEqual(filter.getMaxBalance()));
        }

        Page<Card> cardsPage = cardRepository.findAll(spec, pageable);

        return cardsPage.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BankCardResponse> getAllCards(Pageable pageable) {
        Page<Card> cardsPage = cardRepository.findAll(pageable);
        return cardsPage.map(this::convertToResponse);
    }

    @Transactional(readOnly = true)
    public Page<BankCardResponse> getAllCardsWithFilter(CardFilter filter, Pageable pageable) {
        Specification<Card> spec = Specification.where(null);

        if (filter.getStatus() != null) {
            spec = spec.and(BankCardSpecification.byStatus(filter.getStatus()));
        }

        if (filter.getMinBalance() != null) {
            spec = spec.and(BankCardSpecification.balanceGreaterThanOrEqual(filter.getMinBalance()));
        }

        if (filter.getMaxBalance() != null) {
            spec = spec.and(BankCardSpecification.balanceLessThanOrEqual(filter.getMaxBalance()));
        }

        if (filter.getUserId() != null) {
            spec = spec.and(BankCardSpecification.byUserId(filter.getUserId()));
        }

        Page<Card> cardsPage = cardRepository.findAll(spec, pageable);
        return cardsPage.map(this::convertToResponse);
    }

    @Transactional
    public Card createCard(CreateCardRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        String cardNumber = generateCardNumber();
        String encryptedCardNumber = encryptionService.encrypt(cardNumber);
        String maskedCardNumber = encryptionService.maskCardNumber(cardNumber);

        Card card = Card.builder()
                .cardNumberEncrypted(encryptedCardNumber)
                .cardNumberMasked(maskedCardNumber)
                .cardHolder(request.getCardHolder())
                .expiryDate(LocalDate.now().plusYears(3))
                .cvvEncrypted(encryptionService.encrypt(generateCVV()))
                .status(Card.CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .user(user)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Card savedCard = cardRepository.save(card);

        auditService.logAction(
                AuditService.Actions.CARD_CREATED,
                AuditService.EntityTypes.BANK_CARD,
                savedCard.getId(),
                String.format("Card created for user %s (ID: %d). Holder: %s",
                        user.getUsername(), userId, request.getCardHolder())
        );

        return savedCard;
    }

    @Transactional
    public void transferBetweenCards(CardTransferRequest request, Long userId) {
        try {
            Card fromCard = cardRepository.findByIdAndUserId(request.getFromCardId(), userId)
                    .orElseThrow(() -> new CardNotFoundException(
                            "Source card not found or doesn't belong to user"));

            Card toCard = cardRepository.findByIdAndUserId(request.getToCardId(), userId)
                    .orElseThrow(() -> new CardNotFoundException(
                            "Destination card not found or doesn't belong to user"));

            validateTransfer(fromCard, toCard, request.getAmount());

            fromCard.setBalance(fromCard.getBalance().subtract(request.getAmount()));
            toCard.setBalance(toCard.getBalance().add(request.getAmount()));

            cardRepository.save(fromCard);
            cardRepository.save(toCard);

            CardTransaction transaction = saveTransaction(fromCard, toCard, request.getAmount(),
                    CardTransaction.TransactionStatus.SUCCESS);

            auditService.logAction(
                    AuditService.Actions.TRANSFER_COMPLETED,
                    AuditService.EntityTypes.CARD_TRANSACTION,
                    transaction.getId(),
                    String.format("Transfer from card %d to card %d, amount: %s, user: %d",
                            fromCard.getId(), toCard.getId(), request.getAmount(), userId)
            );

            log.info("Transfer completed: from card {} to card {}, amount: {}, user: {}",
                    fromCard.getId(), toCard.getId(), request.getAmount(), userId);

        } catch (Exception e) {

            auditService.logAction(
                    AuditService.Actions.TRANSFER_FAILED,
                    String.format("Transfer failed: from %d to %d, amount: %s, user: %d, error: %s",
                            request.getFromCardId(), request.getToCardId(),
                            request.getAmount(), userId, e.getMessage())
            );
            throw e;
        }
    }

    @Transactional
    public void requestBlockCard(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card not found or doesn't belong to user"));

        if (card.getStatus() == Card.CardStatus.ACTIVE) {
            card.setStatus(Card.CardStatus.BLOCKED);
            card.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(card);

            auditService.logAction(
                    AuditService.Actions.CARD_BLOCKED,
                    AuditService.EntityTypes.BANK_CARD,
                    cardId,
                    String.format("Card blocked by user %d", userId)
            );

            log.info("Card {} blocked by user {}", cardId, userId);
        }
    }

    @Transactional
    public void activateCard(Long cardId, Long userId) {
        Card card = cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card not found or doesn't belong to user"));

        if (card.getStatus() == Card.CardStatus.BLOCKED &&
            card.getExpiryDate().isAfter(LocalDate.now())) {
            card.setStatus(Card.CardStatus.ACTIVE);
            card.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(card);

            auditService.logAction(
                    AuditService.Actions.CARD_ACTIVATED,
                    AuditService.EntityTypes.BANK_CARD,
                    cardId,
                    String.format("Card activated by user %d", userId)
            );
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBalance(Long userId) {
        BigDecimal total = cardRepository.getTotalBalanceByUserId(userId);

        auditService.logAction(
                AuditService.Actions.BALANCE_CHECKED,
                String.format("Total balance checked by user %d: %s", userId, total)
        );

        return total != null ? total : BigDecimal.ZERO;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void checkExpiredCards() {
        List<Card> expiredCards = cardRepository.findByExpiryDateBefore(LocalDate.now());

        expiredCards.forEach(card -> {
            if (card.getStatus() == Card.CardStatus.ACTIVE) {
                card.setStatus(Card.CardStatus.EXPIRED);
                card.setUpdatedAt(LocalDateTime.now());
                cardRepository.save(card);

                auditService.logAction(
                        "CARD_EXPIRED",
                        AuditService.EntityTypes.BANK_CARD,
                        card.getId(),
                        String.format("Card expired automatically, user: %d",
                                card.getUser().getId())
                );

                log.info("Card {} marked as expired", card.getId());
            }
        });
    }

    @Transactional
    public void blockCardByAdmin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (card.getStatus() == Card.CardStatus.ACTIVE) {
            card.setStatus(Card.CardStatus.BLOCKED);
            card.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(card);

            auditService.logAction(
                    AuditService.Actions.ADMIN_ACTION,
                    AuditService.EntityTypes.BANK_CARD,
                    cardId,
                    "Card blocked by administrator"
            );
        }
    }

    @Transactional
    public void activateCardByAdmin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        if (card.getStatus() == Card.CardStatus.BLOCKED &&
            card.getExpiryDate().isAfter(LocalDate.now())) {
            card.setStatus(Card.CardStatus.ACTIVE);
            card.setUpdatedAt(LocalDateTime.now());
            cardRepository.save(card);

            auditService.logAction(
                    AuditService.Actions.ADMIN_ACTION,
                    AuditService.EntityTypes.BANK_CARD,
                    cardId,
                    "Card activated by administrator"
            );
        }
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        Long userId = card.getUser().getId();

        cardRepository.deleteById(cardId);

        auditService.logAction(
                AuditService.Actions.CARD_DELETED,
                AuditService.EntityTypes.BANK_CARD,
                cardId,
                String.format("Card deleted, user: %d", userId)
        );
    }

    @Transactional(readOnly = true)
    public Card getUserCardById(Long cardId, Long userId) {
        return cardRepository.findByIdAndUserId(cardId, userId)
                .orElseThrow(() -> new CardNotFoundException(
                        "Card not found or doesn't belong to user"));
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        if (fromCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new CardOperationException("Source card is not active");
        }

        if (toCard.getStatus() != Card.CardStatus.ACTIVE) {
            throw new CardOperationException("Destination card is not active");
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CardOperationException("Transfer amount must be positive");
        }
    }

    private CardTransaction saveTransaction(Card fromCard, Card toCard,
                                            BigDecimal amount, CardTransaction.TransactionStatus status) {
        CardTransaction transaction = CardTransaction.builder()
                .fromCard(fromCard)
                .toCard(toCard)
                .amount(amount)
                .status(status)
                .build();

        return transactionRepository.save(transaction);
    }

    String generateCardNumber() {
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            cardNumber.append(random.nextInt(10));
        }
        return cardNumber.toString();
    }

    String generateCVV() {
        return String.format("%03d", random.nextInt(1000));
    }

    public BankCardResponse convertToResponse(Card card) {
        return BankCardResponse.builder()
                .id(card.getId())
                .cardNumberMasked(card.getCardNumberMasked())
                .cardHolder(card.getCardHolder())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}