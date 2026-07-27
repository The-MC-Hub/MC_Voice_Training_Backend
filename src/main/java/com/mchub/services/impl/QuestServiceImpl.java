package com.mchub.services.impl;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.DiscountCode;
import com.mchub.models.User;
import com.mchub.models.UserVoucher;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserVoucherRepository;
import com.mchub.services.QuestService;
import com.mchub.util.EntityUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestServiceImpl implements QuestService {

  public static final List<String> ALL_QUEST_IDS =
      List.of("profile", "practice", "courses", "leaderboard", "reading");

  private final UserRepository userRepository;
  private final DiscountCodeRepository discountCodeRepository;
  private final UserVoucherRepository userVoucherRepository;

  @Override
  public Map<String, Object> getProgress(String userId) {
    User user = EntityUtils.getUserOrThrow(userRepository, userId);

    Set<String> completed = user.getCompletedQuests();
    int total = ALL_QUEST_IDS.size();
    int done = (int) ALL_QUEST_IDS.stream().filter(completed::contains).count();

    return Map.of(
        "completedQuests", completed,
        "totalQuests", total,
        "doneCount", done,
        "allDone", done == total,
        "voucherClaimed", user.isNewbieVoucherClaimed());
  }

  @Override
  public Map<String, Object> completeQuest(String userId, String questId) {
    if (!ALL_QUEST_IDS.contains(questId)) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Unknown quest: " + questId);
    }

    User user = EntityUtils.getUserOrThrow(userRepository, userId);

    user.getCompletedQuests().add(questId);
    userRepository.save(user);

    Set<String> completed = user.getCompletedQuests();
    int done = (int) ALL_QUEST_IDS.stream().filter(completed::contains).count();
    boolean allDone = done == ALL_QUEST_IDS.size();

    return Map.of(
        "questId", questId,
        "completedQuests", completed,
        "doneCount", done,
        "allDone", allDone,
        "voucherClaimed", user.isNewbieVoucherClaimed());
  }

  @Override
  public Map<String, Object> claimVoucher(String userId) {
    User user = EntityUtils.getUserOrThrow(userRepository, userId);

    if (user.isNewbieVoucherClaimed()) {
      throw new AppException(ErrorCode.COUPON_ALREADY_USED, "Newbie voucher đã được nhận rồi.");
    }

    Set<String> completed = user.getCompletedQuests();
    boolean allDone = ALL_QUEST_IDS.stream().allMatch(completed::contains);
    if (!allDone) {
      throw new AppException(
          ErrorCode.VALIDATION_FAILED,
          "Hoàn thành tất cả nhiệm vụ tân binh trước khi nhận voucher.");
    }

    // Generate unique code per user
    String code = "NEWBIE50-" + userId.substring(Math.max(0, userId.length() - 8)).toUpperCase();

    // Upsert: if code already exists in DB (edge case retry), just return it
    DiscountCode existing = discountCodeRepository.findByCodeIgnoreCase(code).orElse(null);
    if (existing == null) {
      DiscountCode voucher =
          DiscountCode.builder()
              .code(code)
              .type(DiscountCode.DiscountType.PERCENT)
              .discountValue(50)
              .applicablePlans(List.of(com.mchub.enums.SubscriptionPlan.BASIC))
              .maxUses(1)
              .usedCount(0)
              .active(true)
              .showInSidebar(false)
              .description("Newbie voucher for user " + userId)
              .expiresAt(LocalDateTime.now().plusDays(30))
              .build();
      discountCodeRepository.save(voucher);
    }

    // Save to user_vouchers collection if not already there
    if (!userVoucherRepository.existsByUserIdAndCode(userId, code)) {
      UserVoucher userVoucher =
          UserVoucher.builder()
              .userId(userId)
              .code(code)
              .discountPercent(50)
              .description("Phần thưởng hoàn thành nhiệm vụ tân binh")
              .source("NEWBIE_QUEST")
              .active(true)
              .expiresAt(LocalDateTime.now().plusDays(30))
              .build();
      userVoucherRepository.save(userVoucher);
    }

    user.setNewbieVoucherClaimed(true);
    userRepository.save(user);

    return Map.of(
        "code", code, "discountPercent", 50, "expiresInDays", 30, "applicablePlan", "BASIC");
  }
}
