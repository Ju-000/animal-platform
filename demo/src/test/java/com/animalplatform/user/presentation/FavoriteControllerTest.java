package com.animalplatform.user.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.animalplatform.favorite.domain.UserFavorite;
import com.animalplatform.favorite.domain.UserFavoriteRepository;
import com.animalplatform.donation.domain.DonationRepository;
import com.animalplatform.user.domain.User;
import com.animalplatform.user.domain.UserRepository;
import com.animalplatform.user.domain.UserRole;
import com.animalplatform.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserFavoriteRepository userFavoriteRepository;

    @Autowired
    private DonationRepository donationRepository;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        donationRepository.deleteAll();
        userFavoriteRepository.deleteAll();
        userRepository.deleteAll();
        owner = userRepository.save(new User(UserRole.MEMBER, "favorite-owner", "owner@example.com", "hash", "Owner", "01011112222", UserStatus.ACTIVE));
        other = userRepository.save(new User(UserRole.MEMBER, "favorite-other", "other@example.com", "hash", "Other", "01033334444", UserStatus.ACTIVE));
    }

    @Test
    void unauthenticatedPostFavoriteReturns401() throws Exception {
        mockMvc.perform(post("/api/favorites/A-001").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserAddsFavoriteAndPersistsIt() throws Exception {
        mockMvc.perform(post("/api/favorites/A-001")
                        .with(csrf())
                        .with(user("favorite-owner").roles("MEMBER")))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(userFavoriteRepository.existsByUserIdAndAnimalNo(owner.getId(), "A-001")).isTrue();
    }

    @Test
    void authenticatedUserCannotDeleteAnotherUsersFavorite() throws Exception {
        userFavoriteRepository.save(new UserFavorite(other.getId(), "A-002"));

        mockMvc.perform(delete("/api/favorites/A-002")
                        .with(csrf())
                        .with(user("favorite-owner").roles("MEMBER")))
                .andExpect(status().isForbidden());
    }
}
