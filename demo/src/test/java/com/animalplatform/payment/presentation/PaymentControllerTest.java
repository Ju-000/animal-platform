package com.animalplatform.payment.presentation;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.payment.application.PortOneClient;
import com.animalplatform.payment.application.PortOneClient.PortOnePayment;
import com.animalplatform.shelter.domain.Shelter;
import com.animalplatform.shelter.domain.ShelterRepository;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShelterRepository shelterRepository;

    @Autowired
    private DonationRepository donationRepository;

    // Replaces the PortOneClient bean in the Spring context so payment verification can run through MockMvc without external API calls.
    @MockitoBean
    private PortOneClient portOneClient;

    private Shelter shelter;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        shelterRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.save(new User(UserRole.MEMBER, "payer", "payer@example.com", "hash", "Payer", "01012345678", UserStatus.ACTIVE));
        shelter = shelterRepository.save(new Shelter("care-1", "Test Shelter", "010", "Seoul", "11", "", ""));
    }

    @Test
    void confirmPaymentWithMatchingAmountMarksDonationPaid() throws Exception {
        String merchantUid = preparePayment("10000");
        when(portOneClient.getPayment("imp-ok"))
                .thenReturn(new PortOnePayment("imp-ok", merchantUid, new BigDecimal("10000"), "paid"));

        mockMvc.perform(post("/api/payments/confirm")
                        .with(csrf())
                        .with(user("payer").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "impUid", "imp-ok",
                                "merchantUid", merchantUid
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.donationId", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        org.assertj.core.api.Assertions.assertThat(donationRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(donationRepository.findAll().getFirst().getPaymentStatus().name()).isEqualTo("PAID");
    }

    @Test
    void confirmPaymentWithMismatchedAmountReturns400AndDoesNotSaveDonation() throws Exception {
        String merchantUid = preparePayment("10000");
        when(portOneClient.getPayment("imp-mismatch"))
                .thenReturn(new PortOnePayment("imp-mismatch", merchantUid, new BigDecimal("9000"), "paid"));
        doNothing().when(portOneClient).cancelPayment(anyString(), anyString(), anyString());

        mockMvc.perform(post("/api/payments/confirm")
                        .with(csrf())
                        .with(user("payer").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "impUid", "imp-mismatch",
                                "merchantUid", merchantUid
                        ))))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(donationRepository.count()).isZero();
    }

    private String preparePayment(String amount) throws Exception {
        String response = mockMvc.perform(post("/api/payments/prepare")
                        .with(csrf())
                        .with(user("payer").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "SHELTER",
                                "donationType", "ONE_TIME",
                                "targetId", shelter.getId(),
                                "orderName", "Shelter Donation",
                                "amount", amount,
                                "buyerName", "Payer",
                                "buyerEmail", "payer@example.com",
                                "buyerTel", "01012345678"
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        return root.path("data").path("merchantUid").asText();
    }
}
