package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CreateCardRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.security.JwtAuthFilter;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CardController.class)
@AutoConfigureMockMvc(addFilters = false)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardService cardService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCard_shouldReturnCreatedCard() throws Exception {
        CreateCardRequest request = new CreateCardRequest();
        request.setCardNumber("1234567812345678");
        request.setOwnerId(1L);
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setInitialBalance(BigDecimal.valueOf(100));

        CardResponse response = CardResponse.builder()
                .id(10L)
                .maskedCardNumber("**** **** **** 5678")
                .ownerUsername("user1")
                .expiryDate(request.getExpiryDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build();

        Mockito.when(cardService.createCard(any(CreateCardRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.maskedCardNumber").value("**** **** **** 5678"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllCards_shouldReturnPage() throws Exception {
        CardResponse c1 = CardResponse.builder()
                .id(1L)
                .maskedCardNumber("**** **** **** 0001")
                .ownerUsername("user1")
                .expiryDate(LocalDate.now().plusYears(1))
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.valueOf(100))
                .build();

        Mockito.when(cardService.getAllCards(any()))
                .thenReturn(new PageImpl<>(List.of(c1), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/cards?page=0&size=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 0001"));
    }
}
