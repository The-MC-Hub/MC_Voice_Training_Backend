package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.MediaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MediaController.class)
@ContextConfiguration(classes = {MediaController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class MediaControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private MediaService mediaService;

    @Nested
    @DisplayName("POST /api/v1/media/upload")
    class UploadMedia {

        @Test
        @DisplayName("200 OK with URL on successful upload")
        void returnsUrlOnSuccess() throws Exception {
            when(mediaService.uploadFile(any(), anyString())).thenReturn("https://cloudinary.com/img.jpg");
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

            mockMvc.perform(multipart("/api/v1/media/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.url").value("https://cloudinary.com/img.jpg"));
        }

        @Test
        @DisplayName("uses default folder 'mchub_chat' when not specified")
        void usesDefaultFolder() throws Exception {
            when(mediaService.uploadFile(any(), anyString())).thenReturn("url");
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

            mockMvc.perform(multipart("/api/v1/media/upload").file(file)).andExpect(status().isOk());

            org.mockito.Mockito.verify(mediaService).uploadFile(any(), org.mockito.ArgumentMatchers.eq("mchub_chat"));
        }

        @Test
        @DisplayName("500 with fail envelope when upload throws — caught inline, not via GlobalExceptionHandler")
        void returns500WithFailEnvelopeOnUploadFailure() throws Exception {
            when(mediaService.uploadFile(any(), anyString())).thenThrow(new java.io.IOException("Cloudinary down"));
            MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

            mockMvc.perform(multipart("/api/v1/media/upload").file(file))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("fail"));
        }
    }
}
