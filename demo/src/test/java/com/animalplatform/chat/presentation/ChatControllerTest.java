package com.animalplatform.chat.presentation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.animalplatform.animal.application.OpenRouterApiException;
import com.animalplatform.chat.application.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Replaces the ChatService bean used by ChatController so MockMvc exercises controller/session/error handling without calling OpenRouter.
    @MockitoBean
    private ChatService chatService;

    @Test
    void openRouterFailureReturns502WithFriendlyMessage() throws Exception {
        when(chatService.reply(anyList()))
                .thenThrow(new OpenRouterApiException("chat", 503, "OpenRouter API unavailable."));

        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "messages", List.of(Map.of("role", "user", "content", "\uc785\uc591 \uc900\ube44\uac00 \ub418\uc5c8\uc744\uae4c\uc694?"))
                        ))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("\ud3ec\ub3d9\uc774\uac00 \uc7a0\uc2dc \uc790\ub9ac\ub97c \ube44\uc6e0\uc5b4\uc694. \uc7a0\uc2dc \ud6c4 \ub2e4\uc2dc \ub9d0\uc744 \uac78\uc5b4\uc8fc\uc138\uc694 \ud83d\udc3e"));
    }

    @Test
    void conversationExceedingSessionLimitReturns429() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("podongChatMessageCount", 20);

        mockMvc.perform(post("/api/chat")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "messages", List.of(Map.of("role", "user", "content", "\ud55c \ubc88 \ub354 \uc0c1\ub2f4\ud574\uc918"))
                        ))))
                .andExpect(status().isTooManyRequests());

        verify(chatService, never()).reply(anyList());
    }
}
