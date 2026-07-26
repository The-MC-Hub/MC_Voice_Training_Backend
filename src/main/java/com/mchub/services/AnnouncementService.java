package com.mchub.services;

import com.mchub.dto.UserResponseDTO;
import com.mchub.models.Announcement;
import java.util.List;
import java.util.Map;

public interface AnnouncementService {
  List<Announcement> getAll();

  List<Announcement> getDrafts();

  Announcement getById(String id);

  Announcement create(Announcement draft);

  Announcement update(String id, Announcement updated);

  void delete(String id);

  int previewRecipients(Announcement.TargetCriteria criteria);

  Announcement send(String id);

  Announcement createAndSend(Announcement announcement);

  Announcement createFromTrigger(
      Announcement.AnnouncementType type,
      String title,
      String emailTitle,
      String content,
      String actionUrl,
      String triggerEvent,
      List<String> targetPlans);

  void triggerNewLessonNotification(String lessonId, String lessonTitle, String lessonCategory);

  void triggerDiscountNotification(String code, int discountPercent, String expiresAtStr);

  String previewEmailHtml(Announcement.EmailDesignData design);

  String renderEmailPreviewRaw(String content, String typeStr);

  String renderEmailPreview(String id);

  Map<String, Object> previewStats(String id);

  List<UserResponseDTO> getUsersByPlan(String plan);

  void approveAndSend(String id);

  void approveAndSend(String id, List<String> overrideRecipientIds);
}
