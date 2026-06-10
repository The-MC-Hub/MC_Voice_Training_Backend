package com.mchub.services;

import com.mchub.dto.EmailCampaignResponseDTO;
import com.mchub.dto.EmailLogDTO;
import com.mchub.dto.EmailTemplateDTO;
import com.mchub.dto.UserPreviewDTO;
import com.mchub.models.EmailTemplate;
import java.util.List;
import java.util.Map;

public interface EmailCampaignService {
    EmailTemplateDTO createTemplate(String name, String subject, String htmlContent, EmailTemplate.DesignData designData);
    List<EmailTemplateDTO> getAllTemplates();
    EmailTemplateDTO updateTemplate(String id, String name, String subject, String htmlContent, EmailTemplate.DesignData designData);
    void deleteTemplate(String id);
    EmailCampaignResponseDTO sendCampaign(String templateId, String subject, String targetType, List<String> targetPlans, List<String> targetRoles, List<String> targetEmails);
    List<EmailCampaignResponseDTO> getAllCampaigns();
    EmailCampaignResponseDTO getCampaignById(String id);
    List<EmailLogDTO> getCampaignLogs(String campaignId);
    void sendTestMail(String templateId, String testEmail);
    int countRecipients(String targetType, List<String> targetPlans, List<String> targetRoles, List<String> targetEmails);
    List<UserPreviewDTO> previewRecipients(String targetType, List<String> targetPlans, List<String> targetRoles, List<String> targetEmails);
}
