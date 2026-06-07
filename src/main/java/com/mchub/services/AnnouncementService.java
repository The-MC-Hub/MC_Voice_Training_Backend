package com.mchub.services;

import com.mchub.dto.UserResponseDTO;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Announcement;
import com.mchub.models.User;
import com.mchub.repositories.AnnouncementRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private static final Logger log = LoggerFactory.getLogger(AnnouncementService.class);

    private final AnnouncementRepository announcementRepo;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<Announcement> getAll() {
        return announcementRepo.findAllByOrderByCreatedAtDesc();
    }

    public List<Announcement> getDrafts() {
        return announcementRepo.findByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus.DRAFT);
    }

    public Announcement getById(String id) {
        return announcementRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Announcement not found: " + id));
    }

    public Announcement create(Announcement draft) {
        draft.setId(null);
        draft.setStatus(Announcement.AnnouncementStatus.DRAFT);
        draft.setCreatedBy(SecurityUtils.getCurrentUserId());
        draft.setRecipientCount(0);
        draft.setSentAt(null);
        return announcementRepo.save(draft);
    }

    public Announcement update(String id, Announcement updated) {
        Announcement existing = getById(id);
        if (existing.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot edit a sent announcement");
        }
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setEmailSubject(updated.getEmailSubject());
        existing.setType(updated.getType());
        existing.setTargetPlans(updated.getTargetPlans());
        existing.setRefId(updated.getRefId());
        existing.setRefType(updated.getRefType());
        return announcementRepo.save(existing);
    }

    public void delete(String id) {
        Announcement existing = getById(id);
        if (existing.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot delete a sent announcement");
        }
        announcementRepo.deleteById(id);
    }

    /** Return users filtered by plan (null = all active users with email). */
    public List<UserResponseDTO> getUsersByPlan(String planStr) {
        List<User> all = userRepository.findAll();
        return all.stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .filter(u -> {
                    if (planStr == null || planStr.isBlank()) return true;
                    return u.getPlan() != null && u.getPlan().name().equalsIgnoreCase(planStr);
                })
                .map(u -> {
                    UserResponseDTO dto = new UserResponseDTO();
                    dto.setId(u.getId());
                    dto.setName(u.getName());
                    dto.setEmail(u.getEmail());
                    dto.setPlan(u.getPlan());
                    return dto;
                })
                .toList();
    }

    /** Overload — send to explicit recipient ID list (null = use announcement's targetPlans). */
    @Async
    public void approveAndSend(String id, List<String> recipientIds) {
        Announcement ann = getById(id);
        if (ann.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Already sent");
        }

        List<User> recipients;
        if (recipientIds != null && !recipientIds.isEmpty()) {
            Set<String> idSet = new java.util.HashSet<>(recipientIds);
            recipients = userRepository.findAll().stream()
                    .filter(u -> idSet.contains(u.getId()))
                    .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                    .toList();
        } else {
            recipients = resolveRecipients(ann);
        }

        ann.setStatus(Announcement.AnnouncementStatus.SENT);
        ann.setSentAt(LocalDateTime.now());
        ann.setRecipientCount(recipients.size());
        announcementRepo.save(ann);

        String subject = ann.getEmailSubject() != null ? ann.getEmailSubject() : ann.getTitle();
        String typeStr = ann.getType() != null ? ann.getType().name() : "GENERAL";
        int sent = 0;
        for (User user : recipients) {
            try {
                String name = user.getName() != null ? user.getName() : "bạn";
                String bodyText = personalizeContent(ann.getContent(), user);
                String html = emailService.buildHtmlEmail(name, bodyText, typeStr);
                emailService.sendHtmlEmail(user.getEmail(), subject, html);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send announcement {} to {}: {}", id, user.getEmail(), SecurityUtils.safeMessage(e));
            }
        }
        log.info("Announcement {} sent to {}/{} recipients", id, sent, recipients.size());
    }

    /**
     * Admin approves and triggers bulk email send. Async — returns immediately after persisting status.
     */
    @Async
    public void approveAndSend(String id) {
        Announcement ann = getById(id);
        if (ann.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Already sent");
        }

        // Fetch target users
        List<User> recipients = resolveRecipients(ann);

        ann.setStatus(Announcement.AnnouncementStatus.SENT);
        ann.setSentAt(LocalDateTime.now());
        ann.setRecipientCount(recipients.size());
        announcementRepo.save(ann);

        String subject = ann.getEmailSubject() != null ? ann.getEmailSubject() : ann.getTitle();
        String typeStr = ann.getType() != null ? ann.getType().name() : "GENERAL";
        int sent = 0;
        for (User user : recipients) {
            try {
                String name = user.getName() != null ? user.getName() : "bạn";
                String bodyText = personalizeContent(ann.getContent(), user);
                String html = emailService.buildHtmlEmail(name, bodyText, typeStr);
                emailService.sendHtmlEmail(user.getEmail(), subject, html);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to send announcement {} to {}: {}", id, user.getEmail(), SecurityUtils.safeMessage(e));
            }
        }
        log.info("Announcement {} sent to {}/{} recipients", id, sent, recipients.size());
    }

    /** Create a draft announcement from a system trigger (e.g. new lesson added). */
    public Announcement createFromTrigger(
            Announcement.AnnouncementType type,
            String title,
            String emailSubject,
            String content,
            String refId,
            String refType,
            List<String> targetPlans) {

        Announcement ann = Announcement.builder()
                .title(title)
                .emailSubject(emailSubject)
                .content(content)
                .type(type)
                .status(Announcement.AnnouncementStatus.DRAFT)
                .targetPlans(targetPlans)
                .refId(refId)
                .refType(refType)
                .recipientCount(0)
                .build();
        return announcementRepo.save(ann);
    }

    /** Returns rendered HTML for the email preview modal (uses sample recipient name). */
    public String renderEmailPreview(String id) {
        Announcement ann = getById(id);
        String typeStr = ann.getType() != null ? ann.getType().name() : "GENERAL";
        String bodyText = personalizeContent(ann.getContent(), buildSampleUser());
        return emailService.buildHtmlEmail("Nguyễn Văn A", bodyText, typeStr);
    }

    /** Render preview from raw form data (not yet saved). */
    public String renderEmailPreviewRaw(String content, String type) {
        String bodyText = content != null ? content.replace("{{name}}", "Nguyễn Văn A").replace("{{email}}", "example@mchub.vn") : "";
        return emailService.buildHtmlEmail("Nguyễn Văn A", bodyText, type != null ? type : "GENERAL");
    }

    public Map<String, Object> previewStats(String id) {
        Announcement ann = getById(id);
        List<User> recipients = resolveRecipients(ann);
        return Map.of(
                "recipientCount", recipients.size(),
                "targetPlans", ann.getTargetPlans() == null || ann.getTargetPlans().isEmpty()
                        ? "Tất cả người dùng" : String.join(", ", ann.getTargetPlans())
        );
    }

    private List<User> resolveRecipients(Announcement ann) {
        List<User> all = userRepository.findAll();
        return all.stream()
                .filter(User::isActive)
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .filter(u -> {
                    if (ann.getTargetPlans() == null || ann.getTargetPlans().isEmpty()) return true;
                    return u.getPlan() != null && ann.getTargetPlans().contains(u.getPlan().name());
                })
                .toList();
    }

    private String personalizeContent(String content, User user) {
        if (content == null) return "";
        return content
                .replace("{{name}}", user.getName() != null ? user.getName() : "bạn")
                .replace("{{email}}", user.getEmail());
    }

    private User buildSampleUser() {
        User u = new User();
        u.setName("Nguyễn Văn A");
        u.setEmail("example@mchub.vn");
        return u;
    }
}
