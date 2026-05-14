package com.animalplatform.adoption.presentation;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.animalplatform.adoption.domain.AdoptionApplication;
import com.animalplatform.adoption.domain.AdoptionApplicationRepository;
import com.animalplatform.adoption.domain.HousingType;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdoptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdoptionApplicationRepository adoptionApplicationRepository;

    private User userOne;
    private User userTwo;

    @BeforeEach
    void setUp() {
        adoptionApplicationRepository.deleteAll();
        userRepository.deleteAll();
        userOne = userRepository.save(new User(UserRole.MEMBER, "adopter-one", "one@example.com", "hash", "One", "01011112222", UserStatus.ACTIVE));
        userTwo = userRepository.save(new User(UserRole.MEMBER, "adopter-two", "two@example.com", "hash", "Two", "01033334444", UserStatus.ACTIVE));
    }

    @Test
    void validApplicationSubmissionSavesPendingEntity() throws Exception {
        mockMvc.perform(post("/api/adoptions")
                        .with(csrf())
                        .with(user("adopter-one").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("ANIMAL-1", "Applicant"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        AdoptionApplication saved = adoptionApplicationRepository.findAll().getFirst();
        org.assertj.core.api.Assertions.assertThat(saved.getAnimalNo()).isEqualTo("ANIMAL-1");
        org.assertj.core.api.Assertions.assertThat(saved.getStatus().name()).isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(saved.getUserId()).isEqualTo(userOne.getId());
    }

    @Test
    void missingApplicantNameReturns400() throws Exception {
        Map<String, Object> request = validRequest("ANIMAL-2", " ");

        mockMvc.perform(post("/api/adoptions")
                        .with(csrf())
                        .with(user("adopter-one").roles("MEMBER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void myApplicationsReturnsOnlyCurrentUsersApplications() throws Exception {
        adoptionApplicationRepository.save(new AdoptionApplication(
                "ANIMAL-1", "One", "01011112222", "one@example.com", "Seoul",
                HousingType.APARTMENT, true, "I am ready.", userOne.getId()
        ));
        adoptionApplicationRepository.save(new AdoptionApplication(
                "ANIMAL-2", "Two", "01033334444", "two@example.com", "Busan",
                HousingType.HOUSE, false, "I am ready too.", userTwo.getId()
        ));

        mockMvc.perform(get("/api/adoptions/my")
                        .with(user("adopter-one").roles("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].animalNo").value("ANIMAL-1"));
    }

    private Map<String, Object> validRequest(String animalNo, String applicantName) {
        return Map.of(
                "animalNo", animalNo,
                "applicantName", applicantName,
                "applicantPhone", "01012345678",
                "applicantEmail", "applicant@example.com",
                "address", "Seoul",
                "housingType", "APARTMENT",
                "hasExperience", true,
                "reason", "I can provide a safe home and long-term care."
        );
    }
}
