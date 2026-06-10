package com.mchub.services.impl;

import com.mchub.dto.EmailCampaignResponseDTO;
import com.mchub.dto.EmailLogDTO;
import com.mchub.dto.EmailTemplateDTO;
import com.mchub.dto.UserPreviewDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.EmailCampaign;
import com.mchub.models.EmailLog;
import com.mchub.models.EmailTemplate;
import com.mchub.models.User;
import com.mchub.repositories.EmailCampaignRepository;
import com.mchub.repositories.EmailLogRepository;
import com.mchub.repositories.EmailTemplateRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.EmailCampaignService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailCampaignServiceImpl implements EmailCampaignService {

    private final EmailTemplateRepository templateRepository;
    private final EmailCampaignRepository campaignRepository;
    private final EmailLogRepository logRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    private String generateHtmlFromDesign(EmailTemplate.DesignData d) {
        String logoHtml = (d.getLogoUrl() != null && !d.getLogoUrl().isBlank())
                ? "<img src=\"" + d.getLogoUrl() + "\" alt=\"Logo\" style=\"max-height:50px;display:block;margin:0 auto 20px auto;\"/>"
                : "";
        String bannerHtml = (d.getBannerUrl() != null && !d.getBannerUrl().isBlank())
                ? "<img src=\"" + d.getBannerUrl() + "\" alt=\"Banner\" style=\"width:100%;max-width:600px;height:auto;display:block;border-radius:8px;margin-bottom:24px;\"/>"
                : "";
        String buttonHtml = (d.getButtonLink() != null && !d.getButtonLink().isBlank()
                && d.getButtonText() != null && !d.getButtonText().isBlank())
                ? "<div style=\"text-align:center;margin-top:32px;margin-bottom:16px;\">"
                  + "<a href=\"" + d.getButtonLink() + "\" target=\"_blank\" "
                  + "style=\"background-color:#f5a623;color:#000;text-decoration:none;padding:12px 28px;"
                  + "font-weight:600;border-radius:6px;display:inline-block;font-size:16px;\">"
                  + d.getButtonText() + "</a></div>"
                : "";
        String title = d.getTitle() != null ? d.getTitle() : "Thông báo từ MC Hub";
        String description = d.getDescription() != null ? d.getDescription() : "";
        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">"
                + "<title>" + title + "</title><style>"
                + "body{margin:0;padding:0;background-color:#f3f4f6;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1f2937;}"
                + ".wrapper{width:100%;background-color:#f3f4f6;padding:40px 0;}"
                + ".container{max-width:600px;margin:0 auto;background-color:#fff;border-radius:12px;overflow:hidden;"
                + "box-shadow:0 10px 15px -3px rgba(0,0,0,0.05),0 4px 6px -2px rgba(0,0,0,0.05);}"
                + ".header{padding:32px 32px 0 32px;text-align:center;}"
                + ".content{padding:0 32px 32px 32px;}"
                + ".title{font-size:24px;font-weight:700;color:#111827;margin-top:0;margin-bottom:16px;line-height:1.3;}"
                + ".description{font-size:16px;line-height:1.6;color:#4b5563;margin-top:0;margin-bottom:20px;white-space:pre-line;}"
                + ".footer{background-color:#f9fafb;padding:24px 32px;text-align:center;font-size:13px;color:#9ca3af;border-top:1px solid #f3f4f6;}"
                + ".footer a{color:#f5a623;text-decoration:none;}"
                + "</style></head><body>"
                + "<div class=\"wrapper\"><div class=\"container\">"
                + "<div class=\"header\">" + logoHtml + "</div>"
                + "<div class=\"content\">" + bannerHtml
                + "<h1 class=\"title\">" + title + "</h1>"
                + "<div class=\"description\">" + description + "</div>"
                + buttonHtml + "</div>"
                + "<div class=\"footer\">"
                + "<p>Đây là email tự động từ hệ thống MC AI Voice Hub.</p>"
                + "<p>Contact: <a href=\"mailto:themchubforwork@gmail.com\">themchubforwork@gmail.com</a></p>"
                + "</div></div></div></body></html>";
    }

    private String replacePlaceholders(String content, String name, String email) {
        if (content == null) return "";
        String result = content;
        result = result.replaceAll("\\{\\{\\s*(tên|họ tên|họ và tên|ten|hoten|fullname|full name|recipient)\\s*\\}\\}", name != null ? name : "");
        result = result.replaceAll("\\{\\{\\s*(email|địa chỉ email|e-mail)\\s*\\}\\}", email != null ? email : "");
        result = result.replaceAll("(?i)\\{\\{\\s*name\\s*\\}\\}", name != null ? name : "");
        result = result.replaceAll("(?i)\\{\\{\\s*email\\s*\\}\\}", email != null ? email : "");
        result = result.replaceAll("\\{\\{[^}]*\\}\\}", "");
        return result;
    }

    private void sendHtmlMail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, "The MC Hub");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send mail to " + toEmail + ": " + e.getMessage(), e);
        }
    }

    @Override
    public EmailTemplateDTO createTemplate(String name, String subject, String htmlContent, EmailTemplate.DesignData designData) {
        String html = (htmlContent != null && !htmlContent.isBlank())
                ? htmlContent
                : generateHtmlFromDesign(designData != null ? designData : EmailTemplate.DesignData.builder().build());
        EmailTemplate template = EmailTemplate.builder()
                .name(name).subject(subject).htmlContent(html).designData(designData).build();
        return toTemplateDTO(templateRepository.save(template));
    }

    @Override
    public List<EmailTemplateDTO> getAllTemplates() {
        return templateRepository.findAll().stream().map(this::toTemplateDTO).collect(Collectors.toList());
    }

    @Override
    public EmailTemplateDTO updateTemplate(String id, String name, String subject, String htmlContent, EmailTemplate.DesignData designData) {
        EmailTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found: " + id));
        template.setName(name);
        template.setSubject(subject);
        template.setDesignData(designData);
        String html = (htmlContent != null && !htmlContent.isBlank())
                ? htmlContent
                : generateHtmlFromDesign(designData != null ? designData : EmailTemplate.DesignData.builder().build());
        template.setHtmlContent(html);
        return toTemplateDTO(templateRepository.save(template));
    }

    @Override
    public void deleteTemplate(String id) {
        if (!templateRepository.existsById(id)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found: " + id);
        }
        templateRepository.deleteById(id);
    }

    // ── Resolve recipients by target config ──────────────────────────────────

    private List<User> resolveRecipients(String targetType, List<String> targetPlans,
                                          List<String> targetRoles, List<String> targetEmails) {
        List<User> raw;
        if ("PLAN".equals(targetType) && targetPlans != null && !targetPlans.isEmpty()) {
            List<SubscriptionPlan> plans = targetPlans.stream()
                    .map(p -> SubscriptionPlan.valueOf(p.toUpperCase()))
                    .collect(Collectors.toList());
            raw = userRepository.findByPlanIn(plans);
        } else if ("ROLE".equals(targetType) && targetRoles != null && !targetRoles.isEmpty()) {
            List<UserRole> roles = targetRoles.stream()
                    .map(r -> UserRole.valueOf(r.toUpperCase()))
                    .collect(Collectors.toList());
            raw = userRepository.findByRoleIn(roles);
        } else if ("PREMIUM".equals(targetType)) {
            raw = userRepository.findByIsPremiumTrue();
        } else if ("CUSTOM".equals(targetType) && targetEmails != null && !targetEmails.isEmpty()) {
            raw = userRepository.findByEmailIn(targetEmails);
        } else {
            raw = userRepository.findAll();
        }
        Set<String> seen = new HashSet<>();
        return raw.stream()
                .filter(u -> u.getEmail() != null && seen.add(u.getEmail().toLowerCase().trim()))
                .collect(Collectors.toList());
    }

    @Override
    public int countRecipients(String targetType, List<String> targetPlans,
                                List<String> targetRoles, List<String> targetEmails) {
        return resolveRecipients(targetType, targetPlans, targetRoles, targetEmails).size();
    }

    @Override
    public List<UserPreviewDTO> previewRecipients(String targetType, List<String> targetPlans,
                                                   List<String> targetRoles, List<String> targetEmails) {
        return resolveRecipients(targetType, targetPlans, targetRoles, targetEmails).stream()
                .map(u -> UserPreviewDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .email(u.getEmail())
                        .plan(u.getPlan() != null ? u.getPlan().name() : null)
                        .role(u.getRole() != null ? u.getRole().name() : null)
                        .isPremium(u.isPremium())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public EmailCampaignResponseDTO sendCampaign(String templateId, String subject,
            String targetType, List<String> targetPlans, List<String> targetRoles, List<String> targetEmails) {
        if (!templateRepository.existsById(templateId)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found: " + templateId);
        }
        String type = targetType != null ? targetType : "ALL";
        List<User> recipients = resolveRecipients(type, targetPlans, targetRoles, targetEmails);
        EmailCampaign campaign = EmailCampaign.builder()
                .templateId(templateId).subject(subject).status("PENDING")
                .totalRecipients(recipients.size())
                .targetType(type)
                .targetPlans(targetPlans)
                .targetRoles(targetRoles)
                .targetEmails(targetEmails)
                .build();
        EmailCampaign saved = campaignRepository.save(campaign);
        processCampaignAsync(saved.getId());
        return toCampaignDTO(saved);
    }

    @Async
    public void processCampaignAsync(String campaignId) {
        EmailCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) return;
        EmailTemplate template = templateRepository.findById(campaign.getTemplateId()).orElse(null);
        if (template == null) {
            campaign.setStatus("FAILED");
            campaignRepository.save(campaign);
            return;
        }
        List<User> users = resolveRecipients(
                campaign.getTargetType(), campaign.getTargetPlans(),
                campaign.getTargetRoles(), campaign.getTargetEmails());
        campaign.setStatus("SENDING");
        campaign.setTotalRecipients(users.size());
        campaignRepository.save(campaign);
        int successCount = 0, failedCount = 0;
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            String name = user.getName() != null ? user.getName() : user.getEmail();
            String email = user.getEmail();
            String finalSubject = replacePlaceholders(campaign.getSubject(), name, email);
            String finalHtml = replacePlaceholders(template.getHtmlContent(), name, email);
            try {
                sendHtmlMail(email, name, finalSubject, finalHtml);
                log.info("[Campaign {}] Sent [{}/{}] -> {}", campaignId, i + 1, users.size(), email);
                logRepository.save(EmailLog.builder().campaignId(campaignId).email(email)
                        .status("SENT").sentAt(LocalDateTime.now()).build());
                successCount++;
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                log.error("[Campaign {}] Failed [{}/{}] -> {}: {}", campaignId, i + 1, users.size(), email, errMsg);
                logRepository.save(EmailLog.builder().campaignId(campaignId).email(email)
                        .status("FAILED").errorReason(errMsg).sentAt(LocalDateTime.now()).build());
                failedCount++;
            }
            campaign.setSuccessCount(successCount);
            campaign.setFailedCount(failedCount);
            campaignRepository.save(campaign);
            if (i < users.size() - 1) {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            }
        }
        campaign.setStatus("COMPLETED");
        campaignRepository.save(campaign);
        log.info("[Campaign {}] Completed. Success: {}, Failed: {}", campaignId, successCount, failedCount);
    }

    @Override
    public List<EmailCampaignResponseDTO> getAllCampaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toCampaignDTO).collect(Collectors.toList());
    }

    @Override
    public EmailCampaignResponseDTO getCampaignById(String id) {
        return toCampaignDTO(campaignRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Campaign not found: " + id)));
    }

    @Override
    public List<EmailLogDTO> getCampaignLogs(String campaignId) {
        return logRepository.findByCampaignIdOrderBySentAtDesc(campaignId).stream()
                .map(this::toLogDTO).collect(Collectors.toList());
    }

    @Override
    public void sendTestMail(String templateId, String testEmail) {
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Template not found: " + templateId));
        User contact = userRepository.findByEmail(testEmail).orElse(null);
        String name = (contact != null && contact.getName() != null)
                ? contact.getName() : "[Hãy điền tên người nhận]";
        String finalSubject = replacePlaceholders(template.getSubject(), name, testEmail);
        String finalHtml = replacePlaceholders(template.getHtmlContent(), name, testEmail);
        sendHtmlMail(testEmail, name, finalSubject, finalHtml);
    }

    private EmailTemplateDTO toTemplateDTO(EmailTemplate t) {
        return EmailTemplateDTO.builder().id(t.getId()).name(t.getName()).subject(t.getSubject())
                .htmlContent(t.getHtmlContent()).designData(t.getDesignData())
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt()).build();
    }

    private EmailCampaignResponseDTO toCampaignDTO(EmailCampaign c) {
        return EmailCampaignResponseDTO.builder().id(c.getId()).templateId(c.getTemplateId())
                .subject(c.getSubject()).status(c.getStatus()).totalRecipients(c.getTotalRecipients())
                .successCount(c.getSuccessCount()).failedCount(c.getFailedCount())
                .createdAt(c.getCreatedAt())
                .targetType(c.getTargetType()).targetPlans(c.getTargetPlans())
                .targetRoles(c.getTargetRoles()).targetEmails(c.getTargetEmails())
                .build();
    }

    private EmailLogDTO toLogDTO(EmailLog l) {
        return EmailLogDTO.builder().id(l.getId()).campaignId(l.getCampaignId())
                .email(l.getEmail()).status(l.getStatus()).errorReason(l.getErrorReason())
                .sentAt(l.getSentAt()).build();
    }
}
