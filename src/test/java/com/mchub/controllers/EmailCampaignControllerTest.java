package com.mchub.controllers;

import com.mchub.dto.EmailCampaignResponseDTO;
import com.mchub.dto.EmailTemplateDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.EmailTemplate;
import com.mchub.services.EmailCampaignService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EmailCampaignController.class)
@ContextConfiguration(classes = {EmailCampaignController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class EmailCampaignControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private EmailCampaignService emailCampaignService;

    @Nested
    @DisplayName("POST/PUT /templates")
    class TemplateEndpoints {

        @Test
        @DisplayName("createTemplate parses designData map into EmailTemplate.DesignData")
        void createsTemplateWithDesignData() throws Exception {
            when(emailCampaignService.createTemplate(eq("Welcome"), eq("Hi"), isNull(), any(EmailTemplate.DesignData.class)))
                    .thenReturn(EmailTemplateDTO.builder().id("tpl-1").build());

            mockMvc.perform(post("/api/v1/admin/email/templates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Welcome\",\"subject\":\"Hi\",\"designData\":{\"title\":\"Hello\"}}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("createTemplate uses empty DesignData default when designData is null")
        void usesEmptyDesignDataWhenNull() throws Exception {
            when(emailCampaignService.createTemplate(any(), any(), any(), any(EmailTemplate.DesignData.class)))
                    .thenReturn(EmailTemplateDTO.builder().id("tpl-1").build());

            mockMvc.perform(post("/api/v1/admin/email/templates")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Welcome\",\"subject\":\"Hi\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /templates/{id}")
    class DeleteTemplate {

        @Test
        @DisplayName("200 OK, delegates to deleteTemplate")
        void deletesTemplate() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/email/templates/{id}", "tpl-1")).andExpect(status().isOk());

            verify(emailCampaignService).deleteTemplate("tpl-1");
        }
    }

    @Nested
    @DisplayName("POST /campaigns/send")
    class SendCampaign {

        @Test
        @DisplayName("defaults targetType to ALL when not specified")
        void defaultsTargetTypeToAll() throws Exception {
            when(emailCampaignService.sendCampaign(eq("tpl-1"), eq("Subject"), eq("ALL"), any(), any(), any()))
                    .thenReturn(EmailCampaignResponseDTO.builder().id("camp-1").status("PENDING").build());

            mockMvc.perform(post("/api/v1/admin/email/campaigns/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateId\":\"tpl-1\",\"subject\":\"Subject\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("forwards explicit targetType and targeting lists")
        void forwardsExplicitTargeting() throws Exception {
            when(emailCampaignService.sendCampaign(eq("tpl-1"), eq("Subject"), eq("PLAN"), any(), any(), any()))
                    .thenReturn(EmailCampaignResponseDTO.builder().id("camp-1").build());

            mockMvc.perform(post("/api/v1/admin/email/campaigns/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateId\":\"tpl-1\",\"subject\":\"Subject\",\"targetType\":\"PLAN\",\"targetPlans\":[\"BASIC\"]}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /campaigns/count-recipients")
    class CountRecipients {

        @Test
        @DisplayName("200 OK, delegates to countRecipients")
        void countsRecipients() throws Exception {
            when(emailCampaignService.countRecipients(eq("ALL"), any(), any(), any())).thenReturn(42);

            mockMvc.perform(post("/api/v1/admin/email/campaigns/count-recipients")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /test-send")
    class TestSend {

        @Test
        @DisplayName("200 OK, delegates to sendTestMail")
        void sendsTestMail() throws Exception {
            mockMvc.perform(post("/api/v1/admin/email/test-send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"templateId\":\"tpl-1\",\"testEmail\":\"qa@test.local\"}"))
                    .andExpect(status().isOk());

            verify(emailCampaignService).sendTestMail("tpl-1", "qa@test.local");
        }
    }

    @Nested
    @DisplayName("GET /campaigns/{id}/logs")
    class GetCampaignLogs {

        @Test
        @DisplayName("200 OK, delegates to getCampaignLogs")
        void returnsLogs() throws Exception {
            when(emailCampaignService.getCampaignLogs("camp-1")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/email/campaigns/{id}/logs", "camp-1")).andExpect(status().isOk());
        }
    }
}
