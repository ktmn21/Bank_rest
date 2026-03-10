package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final CardRepository cardRepository;

    @Transactional
    public void transfer(TransferRequest request, User currentUser) {
        Card from = cardRepository.findById(request.getFromCardId())
                .orElseThrow(() -> new CardNotFoundException("Source card not found"));

        Card to = cardRepository.findById(request.getToCardId())
                .orElseThrow(() -> new CardNotFoundException("Destination card not found"));

        // обе карты должны принадлежать текущему пользователю
        if (!from.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Source card does not belong to you");
        }
        if (!to.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("Destination card does not belong to you");
        }

        if (from.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Source card is not active");
        }
        if (to.getStatus() != CardStatus.ACTIVE) {
            throw new IllegalStateException("Destination card is not active");
        }

        if (from.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(request.getAmount()));
        to.setBalance(to.getBalance().add(request.getAmount()));

        cardRepository.save(from);
        cardRepository.save(to);
    }
}
