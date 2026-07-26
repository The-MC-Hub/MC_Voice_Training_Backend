package com.mchub.services.impl;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.NotificationType;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.mapper.UserMapper;
import com.mchub.models.Announcement;
import com.mchub.models.User;
import com.mchub.repositories.AnnouncementRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.services.AnnouncementService;
import com.mchub.services.EmailService;
import com.mchub.services.NotificationService;
import com.mchub.util.EntityUtils;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Announcement> getAll() {
        return announcementRepo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Announcement> getDrafts() {
        return announcementRepo.findByStatusOrderByCreatedAtDesc(Announcement.AnnouncementStatus.DRAFT);
    }

    @Override
    public Announcement getById(String id) {
        return EntityUtils.getOrThrow(announcementRepo, id, ErrorCode.RESOURCE_NOT_FOUND, "Announcement not found: " + id);
    }

    @Override
    public Announcement create(Announcement draft) {
        draft.setId(null);
        draft.setStatus(Announcement.AnnouncementStatus.DRAFT);
        draft.setCreatedBy(SecurityUtils.getCurrentUserId());
        draft.setRecipientCount(0);
        draft.setSentAt(null);
        return announcementRepo.save(draft);
    }

    @Override
    public Announcement update(String id, Announcement updated) {
        Announcement existing = getById(id);
        if (existing.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot edit a sent announcement");
        }
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setChannel(updated.getChannel());
        existing.setTargetCriteria(updated.getTargetCriteria());
        existing.setEmailDesign(updated.getEmailDesign());
        existing.setActionUrl(updated.getActionUrl());
        existing.setTriggerEvent(updated.getTriggerEvent());
        existing.setTargetPlans(updated.getTargetPlans());
        existing.setRecipientIds(updated.getRecipientIds());
        return announcementRepo.save(existing);
    }

    @Override
    public void delete(String id) {
        Announcement existing = getById(id);
        if (existing.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Cannot delete a sent announcement");
        }
        announcementRepo.deleteById(id);
    }

    @Override
    public int previewRecipients(Announcement.TargetCriteria criteria) {
        return resolveTargetUsers(criteria).size();
    }

    @Override
    public Announcement send(String id) {
        Announcement announcement = getById(id);
        if (announcement.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Announcement already sent");
        }
        dispatchAnnouncement(announcement);
        announcement.setStatus(Announcement.AnnouncementStatus.SENT);
        announcement.setSentAt(LocalDateTime.now());
        return announcementRepo.save(announcement);
    }

    @Override
    public Announcement createAndSend(Announcement announcement) {
        Announcement saved = create(announcement);
        return send(saved.getId());
    }

    @Override
    public Announcement createFromTrigger(Announcement.AnnouncementType type, String title, String emailTitle,
                                          String content, String actionUrl, String triggerEvent, List<String> targetPlans) {
        Announcement.TargetCriteria criteria = new Announcement.TargetCriteria();
        if (targetPlans != null && !targetPlans.isEmpty()) {
            try {
                criteria.setPlan(SubscriptionPlan.valueOf(targetPlans.get(0)));
            } catch (Exception ignored) {}
        }

        Announcement draft = Announcement.builder()
                .title(title)
                .type(type)
                .content(content)
                .channel(Announcement.ChannelType.BOTH)
                .status(Announcement.AnnouncementStatus.DRAFT)
                .createdBy(SecurityUtils.getCurrentUserId())
                .recipientCount(0)
                .actionUrl(actionUrl)
                .triggerEvent(triggerEvent)
                .refId(actionUrl)
                .refType(triggerEvent)
                .targetPlans(targetPlans)
                .targetCriteria(criteria)
                .emailDesign(Announcement.EmailDesignData.builder()
                        .title(emailTitle)
                        .description(content)
                        .buttonText(actionUrl != null ? "Xem ngay" : null)
                        .buttonLink(actionUrl)
                        .build())
                .build();
        return announcementRepo.save(draft);
    }

    @Override
    @Async
    public void triggerNewLessonNotification(String lessonId, String lessonTitle, String lessonCategory) {
        try {
            Announcement.TargetCriteria criteria = new Announcement.TargetCriteria();
            List<User> recipients = resolveTargetUsers(criteria);
            String title = "Bài học mới: " + lessonTitle;
            String message = "Bài học mới thuộc danh mục " + lessonCategory + " vừa được xuất bản! Tham gia luyện tập ngay.";
            String actionUrl = "/m/courses";

            for (User u : recipients) {
                try {
                    notificationService.notify(u.getId(), NotificationType.ANNOUNCEMENT, title, message, actionUrl, false);
                } catch (Exception e) {
                    log.warn("Failed to notify user {}", u.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to trigger new lesson notification", e);
        }
    }

    @Override
    @Async
    public void triggerDiscountNotification(String code, int discountPercent, String expiresAtStr) {
        try {
            Announcement.TargetCriteria criteria = new Announcement.TargetCriteria();
            List<User> recipients = resolveTargetUsers(criteria);
            String title = "Ưu đãi đặc biệt: Giảm " + discountPercent + "% cho bạn!";
            String message = "Nhập mã " + code + " khi thanh toán để nhận ngay ưu đãi " + discountPercent + "%. Hạn dùng: " + expiresAtStr;
            String actionUrl = "/m/pricing";

            for (User u : recipients) {
                try {
                    notificationService.notify(u.getId(), NotificationType.ANNOUNCEMENT, title, message, actionUrl, false);
                } catch (Exception e) {
                    log.warn("Failed to notify user {}", u.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Failed to trigger discount notification", e);
        }
    }

    @Override
    public String previewEmailHtml(Announcement.EmailDesignData design) {
        if (design == null) return "<p>Empty design</p>";
        return emailService.buildHtmlEmail("bạn", design.getDescription(), "GENERAL");
    }

    @Override
    public String renderEmailPreviewRaw(String content, String typeStr) {
        return emailService.buildHtmlEmail("bạn", content != null ? content : "", typeStr);
    }

    @Override
    public String renderEmailPreview(String id) {
        Announcement announcement = getById(id);
        return emailService.buildHtmlEmail("bạn", announcement.getContent(), announcement.getType() != null ? announcement.getType().name() : null);
    }

    @Override
    public Map<String, Object> previewStats(String id) {
        Announcement announcement = getById(id);
        int recipientCount = previewRecipients(announcement.getTargetCriteria());
        String targetPlansLabel;
        if (announcement.getTargetPlans() == null || announcement.getTargetPlans().isEmpty()) {
            targetPlansLabel = "Tất cả người dùng";
        } else {
            targetPlansLabel = String.join(", ", announcement.getTargetPlans());
        }
        return Map.of(
                "id", id,
                "recipientCount", recipientCount,
                "targetPlans", targetPlansLabel,
                "channel", announcement.getChannel() != null ? announcement.getChannel() : Announcement.ChannelType.BOTH,
                "status", announcement.getStatus() != null ? announcement.getStatus() : Announcement.AnnouncementStatus.DRAFT
        );
    }

    @Override
    public List<UserResponseDTO> getUsersByPlan(String planStr) {
        List<User> users = userRepository.findAll();

        return users.stream().filter(u -> {
            if (!u.isActive()) return false;
            if (u.getEmail() == null || u.getEmail().isBlank()) return false;
            if (u.getRole() == UserRole.ADMIN) return false;
            if (planStr != null && !planStr.isBlank() && !"ALL".equalsIgnoreCase(planStr)) {
                try {
                    SubscriptionPlan plan = SubscriptionPlan.valueOf(planStr.toUpperCase());
                    if (u.getPlan() != plan) return false;
                } catch (Exception e) {
                    return false;
                }
            }
            return true;
        }).map(userMapper::toResponseDTO).toList();
    }

    @Override
    @Async
    public void approveAndSend(String id) {
        approveAndSend(id, null);
    }

    @Override
    @Async
    public void approveAndSend(String id, List<String> overrideRecipientIds) {
        Announcement announcement = getById(id);
        if (announcement.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Announcement already sent");
        }

        List<User> recipients;
        if (overrideRecipientIds != null && !overrideRecipientIds.isEmpty()) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> overrideRecipientIds.contains(u.getId()))
                    .toList();
        } else if (announcement.getRecipientIds() != null && !announcement.getRecipientIds().isEmpty()) {
            recipients = userRepository.findAll().stream()
                    .filter(u -> announcement.getRecipientIds().contains(u.getId()))
                    .toList();
        } else {
            recipients = resolveTargetUsers(announcement.getTargetCriteria());
        }

        announcement.setRecipientCount(recipients.size());
        dispatchAnnouncement(announcement, recipients);
        announcement.setStatus(Announcement.AnnouncementStatus.SENT);
        announcement.setSentAt(LocalDateTime.now());
        announcementRepo.save(announcement);
    }

    private List<User> resolveTargetUsers(Announcement.TargetCriteria c) {
        List<User> all = userRepository.findAll();
        if (c == null) return all;

        return all.stream().filter(u -> {
            if (c.getRole() != null && u.getRole() != c.getRole()) return false;
            if (c.getPlan() != null && u.getPlan() != c.getPlan()) return false;
            if (c.getRegisteredAfter() != null && (u.getCreatedAt() == null || u.getCreatedAt().isBefore(c.getRegisteredAfter()))) return false;
            if (c.getRegisteredBefore() != null && (u.getCreatedAt() == null || u.getCreatedAt().isAfter(c.getRegisteredBefore()))) return false;
            return true;
        }).toList();
    }

    private void dispatchAnnouncement(Announcement a) {
        dispatchAnnouncement(a, resolveTargetUsers(a.getTargetCriteria()));
    }

    private void dispatchAnnouncement(Announcement a, List<User> recipients) {
        boolean sendInApp = a.getChannel() == Announcement.ChannelType.IN_APP
                || a.getChannel() == Announcement.ChannelType.BOTH;
        boolean sendEmail = a.getChannel() == Announcement.ChannelType.EMAIL
                || a.getChannel() == Announcement.ChannelType.BOTH;

        for (User u : recipients) {
            if (sendInApp) {
                try {
                    notificationService.notify(u.getId(), NotificationType.ANNOUNCEMENT, a.getTitle(), a.getContent(), a.getActionUrl(), false);
                } catch (Exception e) {
                    log.warn("Failed in-app notify to {}", u.getId(), e);
                }
            }
            if (sendEmail && u.getEmail() != null && !u.getEmail().isBlank()) {
                try {
                    String htmlBody = emailService.buildHtmlEmail(
                            u.getName() != null ? u.getName() : "bạn",
                            a.getContent(),
                            a.getType() != null ? a.getType().name() : null
                    );
                    emailService.sendHtmlEmail(u.getEmail(), a.getTitle(), htmlBody);
                } catch (Exception e) {
                    log.warn("Failed email notify to {}", u.getEmail(), e);
                }
            }
        }
    }
}
