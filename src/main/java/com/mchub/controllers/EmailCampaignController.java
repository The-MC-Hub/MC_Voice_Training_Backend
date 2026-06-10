package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.EmailCampaignResponseDTO;
import com.mchub.dto.EmailLogDTO;
import com.mchub.dto.EmailTemplateDTO;
import com.mchub.dto.UserPreviewDTO;
import com.mchub.models.EmailTemplate;
import com.mchub.services.EmailCampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/email")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class EmailCampaignController {

    private final EmailCampaignService emailCampaignService;

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<EmailTemplateDTO>> createTemplate(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String subject = (String) body.get("subject");
        String htmlContent = (String) body.get("htmlContent");
        EmailTemplate.DesignData designData = parseDesignData(body.get("designData"));
        return ResponseEntity.ok(ApiResponse.success("Template created", emailCampaignService.createTemplate(name, subject, htmlContent, designData)));
    }

    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<EmailTemplateDTO>>> getAllTemplates() {
        return ResponseEntity.ok(ApiResponse.success("Templates fetched", emailCampaignService.getAllTemplates()));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<EmailTemplateDTO>> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String subject = (String) body.get("subject");
        String htmlContent = (String) body.get("htmlContent");
        EmailTemplate.DesignData designData = parseDesignData(body.get("designData"));
        return ResponseEntity.ok(ApiResponse.success("Template updated", emailCampaignService.updateTemplate(id, name, subject, htmlContent, designData)));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable String id) {
        emailCampaignService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted", null));
    }

    @PostMapping("/campaigns/send")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<EmailCampaignResponseDTO>> sendCampaign(@RequestBody Map<String, Object> body) {
        String templateId = (String) body.get("templateId");
        String subject = (String) body.get("subject");
        String targetType = (String) body.getOrDefault("targetType", "ALL");
        List<String> targetPlans = (List<String>) body.get("targetPlans");
        List<String> targetRoles = (List<String>) body.get("targetRoles");
        List<String> targetEmails = (List<String>) body.get("targetEmails");
        return ResponseEntity.ok(ApiResponse.success("Campaign started",
                emailCampaignService.sendCampaign(templateId, subject, targetType, targetPlans, targetRoles, targetEmails)));
    }

    @PostMapping("/campaigns/preview-recipients")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<List<UserPreviewDTO>>> previewRecipients(@RequestBody Map<String, Object> body) {
        String targetType = (String) body.getOrDefault("targetType", "ALL");
        List<String> targetPlans = (List<String>) body.get("targetPlans");
        List<String> targetRoles = (List<String>) body.get("targetRoles");
        List<String> targetEmails = (List<String>) body.get("targetEmails");
        return ResponseEntity.ok(ApiResponse.success("Preview fetched",
                emailCampaignService.previewRecipients(targetType, targetPlans, targetRoles, targetEmails)));
    }

    @PostMapping("/campaigns/count-recipients")
    @SuppressWarnings("unchecked")
    public ResponseEntity<ApiResponse<Integer>> countRecipients(@RequestBody Map<String, Object> body) {
        String targetType = (String) body.getOrDefault("targetType", "ALL");
        List<String> targetPlans = (List<String>) body.get("targetPlans");
        List<String> targetRoles = (List<String>) body.get("targetRoles");
        List<String> targetEmails = (List<String>) body.get("targetEmails");
        return ResponseEntity.ok(ApiResponse.success("Count fetched",
                emailCampaignService.countRecipients(targetType, targetPlans, targetRoles, targetEmails)));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<ApiResponse<List<EmailCampaignResponseDTO>>> getAllCampaigns() {
        return ResponseEntity.ok(ApiResponse.success("Campaigns fetched", emailCampaignService.getAllCampaigns()));
    }

    @GetMapping("/campaigns/{id}")
    public ResponseEntity<ApiResponse<EmailCampaignResponseDTO>> getCampaign(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Campaign fetched", emailCampaignService.getCampaignById(id)));
    }

    @GetMapping("/campaigns/{id}/logs")
    public ResponseEntity<ApiResponse<List<EmailLogDTO>>> getCampaignLogs(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Logs fetched", emailCampaignService.getCampaignLogs(id)));
    }

    @PostMapping("/test-send")
    public ResponseEntity<ApiResponse<Void>> testSend(@RequestBody Map<String, String> body) {
        emailCampaignService.sendTestMail(body.get("templateId"), body.get("testEmail"));
        return ResponseEntity.ok(ApiResponse.success("Test email sent", null));
    }

    @SuppressWarnings("unchecked")
    private EmailTemplate.DesignData parseDesignData(Object raw) {
        if (raw == null) return EmailTemplate.DesignData.builder().build();
        Map<String, String> map = (Map<String, String>) raw;
        return EmailTemplate.DesignData.builder()
                .logoUrl(map.get("logoUrl")).bannerUrl(map.get("bannerUrl"))
                .title(map.get("title")).description(map.get("description"))
                .buttonText(map.get("buttonText")).buttonLink(map.get("buttonLink"))
                .build();
    }
}
