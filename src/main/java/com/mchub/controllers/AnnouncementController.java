package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.UserResponseDTO;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Announcement;
import com.mchub.services.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/announcements")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Announcement>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", announcementService.getAll()));
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponse<List<Announcement>>> getDrafts() {
        return ResponseEntity.ok(ApiResponse.success("OK", announcementService.getDrafts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("OK", announcementService.getById(id)));
    }

    @GetMapping("/{id}/preview-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewStats(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("OK", announcementService.previewStats(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Announcement>> create(@RequestBody Announcement body) {
        if (body.getTitle() == null || body.getTitle().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "title required");
        }
        if (body.getContent() == null || body.getContent().isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "content required");
        }
        if (body.getEmailSubject() == null || body.getEmailSubject().isBlank()) {
            body.setEmailSubject(body.getTitle());
        }
        return ResponseEntity.ok(ApiResponse.success("Created", announcementService.create(body)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Announcement>> update(
            @PathVariable String id, @RequestBody Announcement body) {
        return ResponseEntity.ok(ApiResponse.success("Updated", announcementService.update(id, body)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        announcementService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    /** Returns rendered HTML for email preview modal (uses sample recipient). */
    @GetMapping("/{id}/email-preview")
    public ResponseEntity<String> emailPreview(@PathVariable String id) {
        String html = announcementService.renderEmailPreview(id);
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /** Preview from raw form data (draft not yet saved). */
    @PostMapping("/email-preview-raw")
    public ResponseEntity<String> emailPreviewRaw(@RequestBody java.util.Map<String, String> body) {
        String html = announcementService.renderEmailPreviewRaw(body.get("content"), body.get("type"));
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    /** List users for recipient picker (optionally filter by plan). */
    @GetMapping("/users-by-plan")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getUsersByPlan(
            @RequestParam(required = false) String plan) {
        return ResponseEntity.ok(ApiResponse.success("OK", announcementService.getUsersByPlan(plan)));
    }

    /** Admin approves draft → bulk email send (async). Optional recipientIds to override target list. */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<Void>> send(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body) {
        // Check status synchronously here — approveAndSend() is @Async, so any exception
        // it throws (e.g. "Already sent") never reaches this HTTP response; it only gets
        // logged server-side, and the client sees a false "success".
        Announcement existing = announcementService.getById(id);
        if (existing.getStatus() == Announcement.AnnouncementStatus.SENT) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Announcement already sent");
        }

        @SuppressWarnings("unchecked")
        List<String> recipientIds = body != null ? (List<String>) body.get("recipientIds") : null;
        announcementService.approveAndSend(id, recipientIds);
        return ResponseEntity.ok(ApiResponse.success("Đang gửi email hàng loạt...", null));
    }

    // ── Trigger endpoints — system suggests a draft, admin reviews + approves ──

    @PostMapping("/trigger/new-lesson")
    public ResponseEntity<ApiResponse<Announcement>> triggerNewLesson(@RequestBody Map<String, String> body) {
        String lessonTitle = body.getOrDefault("lessonTitle", "bài học mới");
        String lessonId    = body.get("lessonId");
        String content = "Xin chào {{name}},\n\n"
                + "MC Hub vừa thêm bài luyện tập mới: \"" + lessonTitle + "\".\n\n"
                + "Đăng nhập ngay để luyện tập và nâng cao kỹ năng dẫn chương trình của bạn!\n\n"
                + "Trân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.NEW_LESSON,
                "[Bài học mới] " + lessonTitle,
                "Bài luyện tập mới đã có trên MC Hub!",
                content, lessonId, "VoiceLesson", null);
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }

    @PostMapping("/trigger/discount")
    public ResponseEntity<ApiResponse<Announcement>> triggerDiscount(@RequestBody Map<String, String> body) {
        String planName   = body.getOrDefault("planName", "gói Premium");
        String discountPct = body.getOrDefault("discountPercent", "");
        String code       = body.get("discountCode");
        String content = "Xin chào {{name}},\n\n"
                + "MC Hub đang có chương trình khuyến mãi đặc biệt"
                + (discountPct.isBlank() ? "" : " giảm " + discountPct + "%")
                + " cho " + planName + "!\n\n"
                + (code != null ? "Sử dụng mã: " + code + "\n\n" : "")
                + "Nâng cấp ngay để trải nghiệm đầy đủ tính năng AI coaching.\n\n"
                + "Trân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.DISCOUNT,
                "[Khuyến mãi] " + planName + (discountPct.isBlank() ? "" : " -" + discountPct + "%"),
                "Ưu đãi đặc biệt dành riêng cho bạn!",
                content, code, "DiscountCode", List.of("FREE"));
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }

    @PostMapping("/trigger/maintenance")
    public ResponseEntity<ApiResponse<Announcement>> triggerMaintenance(@RequestBody Map<String, String> body) {
        String timeDesc = body.getOrDefault("time", "trong thời gian tới");
        String duration = body.getOrDefault("duration", "");
        String content = "Xin chào {{name}},\n\n"
                + "Hệ thống MC Hub sẽ bảo trì " + timeDesc
                + (duration.isBlank() ? "" : " (khoảng " + duration + ")")
                + ".\n\nTrong thời gian này, một số tính năng có thể tạm thời không hoạt động.\n\n"
                + "Chúng tôi xin lỗi vì sự bất tiện này. Cảm ơn bạn đã thông cảm!\n\n"
                + "Trân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.MAINTENANCE,
                "[Bảo trì] Hệ thống bảo trì " + timeDesc,
                "Thông báo bảo trì hệ thống MC Hub",
                content, null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }

    @PostMapping("/trigger/social-post")
    public ResponseEntity<ApiResponse<Announcement>> triggerSocialPost(@RequestBody Map<String, String> body) {
        String postTitle = body.getOrDefault("postTitle", "bài đăng mới");
        String postUrl   = body.get("postUrl");
        String content = "Xin chào {{name}},\n\n"
                + "MC Hub vừa đăng bài mới: \"" + postTitle + "\".\n\n"
                + (postUrl != null ? "Xem ngay tại: " + postUrl + "\n\n" : "")
                + "Đừng quên theo dõi fanpage để không bỏ lỡ những nội dung bổ ích!\n\n"
                + "Trân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.SOCIAL_POST,
                "[Facebook] " + postTitle,
                "Bài viết mới từ MC Hub!",
                content, postUrl, "SocialPost", null);
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }

    @PostMapping("/trigger/feature-update")
    public ResponseEntity<ApiResponse<Announcement>> triggerFeatureUpdate(@RequestBody Map<String, String> body) {
        String featureName = body.getOrDefault("featureName", "tính năng mới");
        String content = "Xin chào {{name}},\n\n"
                + "MC Hub vừa ra mắt " + featureName + "!\n\n"
                + body.getOrDefault("description", "Đăng nhập để khám phá ngay nhé.")
                + "\n\nTrân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.FEATURE_UPDATE,
                "[Tính năng mới] " + featureName,
                "Cập nhật mới trên MC Hub!",
                content, null, null, null);
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }

    @PostMapping("/trigger/competition")
    public ResponseEntity<ApiResponse<Announcement>> triggerCompetition(@RequestBody Map<String, String> body) {
        String compName = body.getOrDefault("competitionName", "cuộc thi mới");
        String content = "Xin chào {{name}},\n\n"
                + "MC Hub mở cuộc thi: \"" + compName + "\"!\n\n"
                + body.getOrDefault("description", "Tham gia ngay để thể hiện tài năng và giành phần thưởng hấp dẫn.")
                + "\n\nTrân trọng,\nĐội ngũ MC Hub";
        Announcement ann = announcementService.createFromTrigger(
                Announcement.AnnouncementType.COMPETITION,
                "[Thi đấu] " + compName,
                "Cuộc thi mới đã mở đăng ký!",
                content, body.get("competitionId"), "Competition", null);
        return ResponseEntity.ok(ApiResponse.success("Draft created", ann));
    }
}
