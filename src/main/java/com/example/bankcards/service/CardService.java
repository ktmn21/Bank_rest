package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardEncryptionUtil encryptionUtil;

    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        User owner = userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found with id: " + request.getOwnerId()));

        String encryptedNumber = encryptionUtil.encrypt(request.getCardNumber());

        if (cardRepository.existsByCardNumber(encryptedNumber)) {
            throw new DuplicateCardException("Card with this number already exists");
        }

        Card card = Card.builder()
                .cardNumber(encryptedNumber)
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .build();

        Card saved = cardRepository.save(card);

        return CardResponse.builder()
                .id(saved.getId())
                .maskedCardNumber(encryptionUtil.maskCardNumber(saved.getCardNumber()))
                .ownerUsername(saved.getOwner().getUsername())
                .expiryDate(saved.getExpiryDate())
                .status(saved.getStatus())
                .balance(saved.getBalance())
                .build();
    }

    @Transactional
    public CardResponse blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        card.setStatus(CardStatus.BLOCKED);
        Card saved = cardRepository.save(card);

        return CardResponse.builder()
                .id(saved.getId())
                .maskedCardNumber(encryptionUtil.maskCardNumber(saved.getCardNumber()))
                .ownerUsername(saved.getOwner().getUsername())
                .expiryDate(saved.getExpiryDate())
                .status(saved.getStatus())
                .balance(saved.getBalance())
                .build();
    }

    @Transactional
    public CardResponse activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        card.setStatus(CardStatus.ACTIVE);
        Card saved = cardRepository.save(card);

        return CardResponse.builder()
                .id(saved.getId())
                .maskedCardNumber(encryptionUtil.maskCardNumber(saved.getCardNumber()))
                .ownerUsername(saved.getOwner().getUsername())
                .expiryDate(saved.getExpiryDate())
                .status(saved.getStatus())
                .balance(saved.getBalance())
                .build();
    }

    @Transactional
    public CardResponse requestBlock(Long cardId, User currentUser) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You don't own this card");
        }

        card.setStatus(CardStatus.BLOCKED);
        Card saved = cardRepository.save(card);

        return CardResponse.builder()
                .id(saved.getId())
                .maskedCardNumber(encryptionUtil.maskCardNumber(saved.getCardNumber()))
                .ownerUsername(saved.getOwner().getUsername())
                .expiryDate(saved.getExpiryDate())
                .status(saved.getStatus())
                .balance(saved.getBalance())
                .build();
    }

    public Page<CardResponse> getMyCards(User user, CardStatus status, Pageable pageable) {
        Page<Card> page;

        if (status == null) {
            page = cardRepository.findByOwner(user, pageable);
        } else {
            page = cardRepository.findByOwnerAndStatus(user, status, pageable);
        }

        return page.map(card -> CardResponse.builder()
                .id(card.getId())
                .maskedCardNumber(encryptionUtil.maskCardNumber(card.getCardNumber()))
                .ownerUsername(card.getOwner().getUsername())
                .expiryDate(card.getExpiryDate())
                .status(card.getStatus())
                .balance(card.getBalance())
                .build()
        );
    }

    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(card -> CardResponse.builder()
                        .id(card.getId())
                        .maskedCardNumber(encryptionUtil.maskCardNumber(card.getCardNumber()))
                        .ownerUsername(card.getOwner().getUsername())
                        .expiryDate(card.getExpiryDate())
                        .status(card.getStatus())
                        .balance(card.getBalance())
                        .build());
    }

    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));
        cardRepository.delete(card);
    }


}
