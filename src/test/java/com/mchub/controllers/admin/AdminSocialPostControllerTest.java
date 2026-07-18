package com.mchub.controllers.admin;

import com.mchub.dto.SocialPostDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.SocialPostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminSocialPostController.class)
@ContextConfiguration(classes = {AdminSocialPostController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AdminSocialPostControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private SocialPostService socialPostService;

    @Nested
    @DisplayName("POST /api/v1/admin/social-posts")
    class Create {

        @Test
        @DisplayName("400 when image is blank — bean validation")
        void rejectsBlankImage() throws Exception {
            mockMvc.perform(post("/api/v1/admin/social-posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"image\":\"\",\"fbLink\":\"https://fb.com/x\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 when fbLink is blank — bean validation")
        void rejectsBlankFbLink() throws Exception {
            mockMvc.perform(post("/api/v1/admin/social-posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"image\":\"img.jpg\",\"fbLink\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK with valid payload")
        void createsWithValidPayload() throws Exception {
            when(socialPostService.createPost(any())).thenReturn(SocialPostDTO.builder().id("new-id").build());

            mockMvc.perform(post("/api/v1/admin/social-posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"image\":\"img.jpg\",\"fbLink\":\"https://fb.com/x\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/social-posts/{id}")
    class Delete {

        @Test
        @DisplayName("200 OK, delegates to deletePost")
        void deletesPost() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/social-posts/{id}", "post-1"))
                    .andExpect(status().isOk());

            verify(socialPostService).deletePost("post-1");
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/admin/social-posts/{id}/toggle")
    class Toggle {

        @Test
        @DisplayName("200 OK, delegates to toggleActive")
        void togglesActive() throws Exception {
            when(socialPostService.toggleActive("post-1")).thenReturn(SocialPostDTO.builder().id("post-1").active(false).build());

            mockMvc.perform(patch("/api/v1/admin/social-posts/{id}/toggle", "post-1"))
                    .andExpect(status().isOk());
        }
    }
}
