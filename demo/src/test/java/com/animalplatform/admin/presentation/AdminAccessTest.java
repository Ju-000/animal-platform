package com.animalplatform.admin.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.animalplatform.statistics.application.BatchCollectorService;
import com.animalplatform.statistics.application.BatchCollectorService.BatchRunResult;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AdminAccessTest {

    @Autowired
    private MockMvc mockMvc;

    // Replaces the real batch collector so the admin endpoint security/response can be verified without calling data.go.kr.
    @MockitoBean
    private BatchCollectorService batchCollectorService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessMonitor() throws Exception {
        mockMvc.perform(get("/api/admin/monitor"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCannotAccessAdminMonitor() throws Exception {
        mockMvc.perform(get("/api/admin/monitor"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void memberCannotAccessAdminMonitor() throws Exception {
        mockMvc.perform(get("/api/admin/monitor"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanRunBatchEndpoint() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        when(batchCollectorService.collectDailySnapshots())
                .thenReturn(new BatchRunResult(0, 0, 0, 0, now, now, "SUCCESS"));

        mockMvc.perform(post("/api/admin/batch/run").with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerUiIsAvailableInLocalProfile() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }
}
