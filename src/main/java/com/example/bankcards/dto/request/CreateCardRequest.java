package com.example.bankcards.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {

    @NotBlank(message = "Card holder is required")
    @Size(min = 2, max = 100, message = "Card holder must be between 2 and 100 characters")
    private String cardHolder;

    private Long userId;
}