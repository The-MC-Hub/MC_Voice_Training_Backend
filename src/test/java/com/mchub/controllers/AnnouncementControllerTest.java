package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.Announcement;
import com.mchub.services.AnnouncementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AnnouncementControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AnnouncementService announcementService;

    @Nested
    @DisplayName("POST /api/v1/admin/announcements — validation")
    class Create {

        @Test
        @DisplayName("400 VALIDATION_FAILED when title is blank")
        void rejectsBlankTitle() throws Exception {
            mockMvc.perform(post("/api/v1/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"content\":\"Body\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when content is blank")
        void rejectsBlankContent() throws Exception {
            mockMvc.perform(post("/api/v1/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"content\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("defaults emailSubject to title when not supplied")
        void defaultsEmailSubjectToTitle() throws Exception {
            when(announcementService.create(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"My Title\",\"content\":\"Body\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.emailSubject").value("My Title"));
        }

        @Test
        @DisplayName("preserves explicit emailSubject when supplied")
        void preservesExplicitEmailSubject() throws Exception {
            when(announcementService.create(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/admin/announcements")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Title\",\"content\":\"Body\",\"emailSubject\":\"Custom Subject\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.emailSubject").value("Custom Subject"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/announcements/{id}/send")
    class Send {

        @Test
        @DisplayName("passes null recipientIds when body is omitted")
        void passesNullRecipientIdsWhenBodyOmitted() throws Exception {
            mockMvc.perform(post("/api/v1/admin/announcements/{id}/send", "ann-1"))
                    .andExpect(status().isOk());

            verify(announcementService).approveAndSend(eq("ann-1"), isNull());
        }

        @Test
        @DisplayName("forwards recipientIds when present in body")
        void forwardsRecipientIdsWhenPresent() throws Exception {
            mockMvc.perform(post("/api/v1/admin/announcements/{id}/send", "ann-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"recipientIds\":[\"u1\",\"u2\"]}"))
                    .andExpect(status().isOk());

            verify(announcementService).approveAndSend(eq("ann-1"), eq(java.util.List.of("u1", "u2")));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/announcements/{id}/email-preview — raw HTML response")
    class EmailPreview {

        @Test
        @DisplayName("returns raw HTML with text/html content-type, not wrapped in ApiResponse")
        void returnsRawHtml() throws Exception {
            when(announcementService.renderEmailPreview("ann-1")).thenReturn("<html><body>Preview</body></html>");

            mockMvc.perform(get("/api/v1/admin/announcements/{id}/email-preview", "ann-1"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("<html><body>Preview</body></html>"));
        }
    }

    @Nested
    @DisplayName("POST /trigger/new-lesson — draft content generation")
    class TriggerNewLesson {

        @Test
        @DisplayName("uses default lesson title when not supplied")
        void usesDefaultLessonTitle() throws Exception {
            when(announcementService.createFromTrigger(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Announcement.builder().id("ann-1").build());

            mockMvc.perform(post("/api/v1/admin/announcements/trigger/new-lesson")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            verify(announcementService).createFromTrigger(
                    eq(Announcement.AnnouncementType.NEW_LESSON),
                    eq("[Bài học mới] bài học mới"),
                    any(), any(), isNull(), eq("VoiceLesson"), isNull());
        }
    }

    @Nested
    @DisplayName("POST /trigger/discount — targets FREE plan by default")
    class TriggerDiscount {

        @Test
        @DisplayName("hardcodes targetPlans=[FREE] regardless of body content")
        void alwaysTargetsFreePlan() throws Exception {
            when(announcementService.createFromTrigger(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Announcement.builder().id("ann-1").build());

            mockMvc.perform(post("/api/v1/admin/announcements/trigger/discount")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"planName\":\"BASIC\",\"discountCode\":\"SAVE20\"}"))
                    .andExpect(status().isOk());

            verify(announcementService).createFromTrigger(
                    eq(Announcement.AnnouncementType.DISCOUNT), any(), any(), any(),
                    eq("SAVE20"), eq("DiscountCode"), eq(java.util.List.of("FREE")));
        }
    }
}
