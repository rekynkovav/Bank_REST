package com.example.bankcards.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.CreditCardNumber;

import java.math.BigDecimal;
import java.time.YearMonth;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

    @NotBlank(message = "Card holder is required")
    @Size(min = 2, max = 100, message = "Card holder must be between 2 and 100 characters")
    private String cardHolder;

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must be 16 digits")
    @CreditCardNumber(message = "Invalid card number")
    private String cardNumber;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Card must not be expired")
    @JsonFormat(pattern = "MM/yy")
    private YearMonth expiryDate;

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3 or 4 digits")
    private String cvv;

    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull(message = "User ID is required")
    private Long userId;
}