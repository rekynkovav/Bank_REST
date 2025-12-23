package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankCardRepository extends JpaRepository<Card, Long>,
        JpaSpecificationExecutor<Card> {

    Optional<Card> findByIdAndUserId(Long id, Long userId);

    Page<Card> findAllByUserId(Long userId, Pageable pageable);

    List<Card> findByUserId(Long userId);

    @Query("SELECT c FROM Card c WHERE c.user.id = :userId AND c.status = 'ACTIVE'")
    List<Card> findActiveCardsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(c.balance) FROM Card c WHERE c.user.id = :userId")
    BigDecimal getTotalBalanceByUserId(@Param("userId") Long userId);

    List<Card> findByExpiryDateBefore(java.time.LocalDate date);
}