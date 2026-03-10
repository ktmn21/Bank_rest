package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class CardResponse {
    private Long id;
    private String maskedCardNumber;
    private String ownerUsername;
    private LocalDate expiryDate;
    private CardStatus status;
    private BigDecimal balance;
}
