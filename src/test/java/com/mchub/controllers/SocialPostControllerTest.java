package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.SocialPostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SocialPostController.class)
@ContextConfiguration(classes = {SocialPostController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class SocialPostControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SocialPostService socialPostService;

    @Nested
    @DisplayName("GET /api/v1/social-posts — public")
    class GetActive {

        @Test
        @DisplayName("200 OK, delegates to getActivePosts")
        void returnsActivePosts() throws Exception {
            when(socialPostService.getActivePosts()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/social-posts")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/social-posts/{id}/click")
    class RecordClick {

        @Test
        @DisplayName("200 OK, delegates to recordClick")
        void recordsClick() throws Exception {
            mockMvc.perform(post("/api/v1/social-posts/{id}/click", "post-1")).andExpect(status().isOk());

            verify(socialPostService).recordClick("post-1");
        }
    }
}
