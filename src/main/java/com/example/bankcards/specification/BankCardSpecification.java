package com.example.bankcards.specification;

import com.example.bankcards.entity.Card;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class BankCardSpecification {

    public static Specification<Card> byUserId(Long userId) {
        return (root, query, cb) ->
                userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Card> byStatus(Card.CardStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Card> balanceGreaterThanOrEqual(BigDecimal minBalance) {
        return (root, query, cb) ->
                minBalance == null ? null : cb.greaterThanOrEqualTo(root.get("balance"), minBalance);
    }

    public static Specification<Card> balanceLessThanOrEqual(BigDecimal maxBalance) {
        return (root, query, cb) ->
                maxBalance == null ? null : cb.lessThanOrEqualTo(root.get("balance"), maxBalance);
    }
}