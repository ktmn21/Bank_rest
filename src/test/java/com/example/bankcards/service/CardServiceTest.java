package com.example.bankcards.service;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardEncryptionUtil;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.exception.DuplicateCardException;

import java.math.BigDecimal;



import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardEncryptionUtil encryptionUtil;

    @InjectMocks
    private CardService cardService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@test.com")
                .password("encoded")
                .role(Role.USER)
                .build();
    }

    @Test
    void createCard_shouldCreateCard_whenValidRequest() {
        // given
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");
        request.setOwnerId(1L);
        request.setExpiryDate(LocalDate.now().plusYears(2));
        request.setInitialBalance(BigDecimal.valueOf(1000));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(encryptionUtil.encrypt("1234567812345678")).thenReturn("encrypted-card");
        when(cardRepository.existsByCardNumber("encrypted-card")).thenReturn(false);

        Card saved = Card.builder()
                .id(10L)
                .cardNumber("encrypted-card")
                .owner(owner)
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(request.getInitialBalance())
                .build();

        when(cardRepository.save(any(Card.class))).thenReturn(saved);
        when(encryptionUtil.maskCardNumber("encrypted-card")).thenReturn("**** **** **** 5678");

        // when
        CardResponse response = cardService.createCard(request);

        // then
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 5678");
        assertThat(response.getOwnerUsername()).isEqualTo("user1");
        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.getBalance()).isEqualByComparingTo("1000");

        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_shouldThrowDuplicate_whenCardNumberAlreadyExists() {
        // given
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");
        request.setOwnerId(1L);
        request.setExpiryDate(LocalDate.now().plusYears(2));
        request.setInitialBalance(BigDecimal.ZERO);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(encryptionUtil.encrypt("1234567812345678")).thenReturn("encrypted-card");
        when(cardRepository.existsByCardNumber("encrypted-card")).thenReturn(true);

        // when / then
        assertThatThrownBy(() -> cardService.createCard(request))
                .isInstanceOf(DuplicateCardException.class)
                .hasMessageContaining("already exists");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void blockCard_shouldSetStatusBlocked_whenCardExists() {
        // given
        Card card = Card.builder()
                .id(1L)
                .cardNumber("encrypted-card")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionUtil.maskCardNumber("encrypted-card")).thenReturn("**** **** **** 5678");

        // when
        CardResponse response = cardService.blockCard(1L);

        // then
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(card);
    }

    @Test
    void blockCard_shouldThrow_whenCardNotFound() {
        // given
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> cardService.blockCard(99L))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void activateCard_shouldSetStatusActive_whenCardExists() {
        // given
        Card card = Card.builder()
                .id(2L)
                .cardNumber("encrypted-card-2")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.valueOf(200))
                .build();

        when(cardRepository.findById(2L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionUtil.maskCardNumber("encrypted-card-2")).thenReturn("**** **** **** 9999");

        // when
        CardResponse response = cardService.activateCard(2L);

        // then
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(cardRepository).save(card);
    }

    @Test
    void requestBlock_shouldBlockOwnCard_whenUserIsOwner() {
        // given
        Card card = Card.builder()
                .id(3L)
                .cardNumber("encrypted-own-card")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(300))
                .build();

        when(cardRepository.findById(3L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encryptionUtil.maskCardNumber("encrypted-own-card"))
                .thenReturn("**** **** **** 0001");

        // when
        CardResponse response = cardService.requestBlock(3L, owner);

        // then
        assertThat(card.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(cardRepository).save(card);
    }

    @Test
    void requestBlock_shouldThrowUnauthorized_whenUserIsNotOwner() {
        // given
        User otherUser = User.builder()
                .id(2L)
                .username("other")
                .email("other@test.com")
                .password("pwd")
                .role(Role.USER)
                .build();

        Card card = Card.builder()
                .id(4L)
                .cardNumber("encrypted-card-other")
                .owner(otherUser)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build();

        when(cardRepository.findById(4L)).thenReturn(Optional.of(card));

        // when / then
        assertThatThrownBy(() -> cardService.requestBlock(4L, owner))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You don't own this card");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getMyCards_shouldReturnAllCardsForUser_whenStatusIsNull() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        Card card1 = Card.builder()
                .id(1L)
                .cardNumber("enc-1")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build();

        Card card2 = Card.builder()
                .id(2L)
                .cardNumber("enc-2")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(2))
                .status(CardStatus.BLOCKED)
                .balance(BigDecimal.valueOf(200))
                .build();

        when(cardRepository.findByOwner(owner, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(card1, card2)));

        when(encryptionUtil.maskCardNumber("enc-1")).thenReturn("**** **** **** 0001");
        when(encryptionUtil.maskCardNumber("enc-2")).thenReturn("**** **** **** 0002");

        // when
        Page<CardResponse> page = cardService.getMyCards(owner, null, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getMaskedCardNumber()).isEqualTo("**** **** **** 0001");
        assertThat(page.getContent().get(1).getStatus()).isEqualTo(CardStatus.BLOCKED);

        verify(cardRepository).findByOwner(owner, pageable);
    }

    @Test
    void getMyCards_shouldFilterByStatus_whenStatusProvided() {
        // given
        PageRequest pageable = PageRequest.of(0, 10);

        Card card1 = Card.builder()
                .id(3L)
                .cardNumber("enc-3")
                .owner(owner)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(300))
                .build();

        when(cardRepository.findByOwnerAndStatus(owner, CardStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(java.util.List.of(card1)));

        when(encryptionUtil.maskCardNumber("enc-3")).thenReturn("**** **** **** 0003");

        // when
        Page<CardResponse> page = cardService.getMyCards(owner, CardStatus.ACTIVE, pageable);

        // then
        assertThat(page.getTotalElements()).isEqualTo(1);
        CardResponse response = page.getContent().get(0);
        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.getMaskedCardNumber()).isEqualTo("**** **** **** 0003");

        verify(cardRepository).findByOwnerAndStatus(owner, CardStatus.ACTIVE, pageable);
    }



}
