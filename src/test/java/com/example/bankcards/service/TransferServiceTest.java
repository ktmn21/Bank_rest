package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransferService transferService;

    private User user;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("user1")
                .email("user1@test.com")
                .password("pwd")
                .role(Role.USER)
                .build();

        fromCard = Card.builder()
                .id(10L)
                .cardNumber("enc-from")
                .owner(user)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(1000))
                .build();

        toCard = Card.builder()
                .id(20L)
                .cardNumber("enc-to")
                .owner(user)
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(500))
                .build();
    }

    @Test
    void transfer_shouldMoveMoneyBetweenOwnActiveCards_whenEnoughBalance() {
        // given
        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(20L);
        request.setAmount(BigDecimal.valueOf(200));

        when(cardRepository.findById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(toCard));

        // when
        transferService.transfer(request, user);

        // then
        assertThat(fromCard.getBalance()).isEqualByComparingTo("800");
        assertThat(toCard.getBalance()).isEqualByComparingTo("700");
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void transfer_shouldThrow_whenSourceCardNotFound() {
        // given
        TransferRequest request = new TransferRequest();
        request.setFromCardId(999L);
        request.setToCardId(20L);
        request.setAmount(BigDecimal.valueOf(100));

        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> transferService.transfer(request, user))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Source card not found");
    }

    @Test
    void transfer_shouldThrow_whenDestinationCardNotFound() {
        // given
        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(999L);
        request.setAmount(BigDecimal.valueOf(100));

        when(cardRepository.findById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(999L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> transferService.transfer(request, user))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Destination card not found");
    }

    @Test
    void transfer_shouldThrow_whenCardsDoNotBelongToUser() {
        // given
        User other = User.builder()
                .id(2L)
                .username("other")
                .email("other@test.com")
                .password("pwd")
                .role(Role.USER)
                .build();

        fromCard.setOwner(other);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(20L);
        request.setAmount(BigDecimal.valueOf(100));

        when(cardRepository.findById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(toCard));

        // when / then
        assertThatThrownBy(() -> transferService.transfer(request, user))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("does not belong to you");

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transfer_shouldThrow_whenInsufficientFunds() {
        // given
        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(20L);
        request.setAmount(BigDecimal.valueOf(5000));

        when(cardRepository.findById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(toCard));

        // when / then
        assertThatThrownBy(() -> transferService.transfer(request, user))
                .isInstanceOf(InsufficientFundsException.class);

        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transfer_shouldThrow_whenAnyCardIsNotActive() {
        // given
        fromCard.setStatus(CardStatus.BLOCKED);

        TransferRequest request = new TransferRequest();
        request.setFromCardId(10L);
        request.setToCardId(20L);
        request.setAmount(BigDecimal.valueOf(100));

        when(cardRepository.findById(10L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(20L)).thenReturn(Optional.of(toCard));

        // when / then
        assertThatThrownBy(() -> transferService.transfer(request, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Source card is not active");

        verify(cardRepository, never()).save(any(Card.class));
    }
}
