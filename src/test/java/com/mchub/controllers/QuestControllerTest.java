package com.mchub.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.services.QuestService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = QuestController.class)
@ContextConfiguration(classes = {QuestController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class QuestControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private QuestService questService;

  private static final String USER_ID = "user-quest-001";

  @BeforeEach
  void setUp() {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Nested
  @DisplayName("GET /api/v1/quests/progress")
  class GetProgress {

    @Test
    @DisplayName("reports doneCount/allDone correctly for partial completion")
    void reportsPartialCompletion() throws Exception {
      Map<String, Object> progress =
          Map.of(
              "quests",
              List.of(),
              "doneCount",
              1,
              "totalQuests",
              5,
              "allDone",
              false,
              "claimedVoucher",
              false);
      when(questService.getProgress(USER_ID)).thenReturn(progress);

      mockMvc
          .perform(get("/api/v1/quests/progress"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.doneCount").value(1))
          .andExpect(jsonPath("$.data.totalQuests").value(5))
          .andExpect(jsonPath("$.data.allDone").value(false));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/quests/complete/{questId}")
  class CompleteQuest {

    @Test
    @DisplayName("400 VALIDATION_FAILED for an unknown quest id")
    void rejectsUnknownQuestId() throws Exception {
      when(questService.completeQuest(USER_ID, "bogus-quest"))
          .thenThrow(
              new AppException(ErrorCode.VALIDATION_FAILED, "Nhiệm vụ không hợp lệ: bogus-quest"));

      mockMvc
          .perform(post("/api/v1/quests/complete/{questId}", "bogus-quest"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("idempotent — completing the same quest twice does not duplicate in the set")
    void isIdempotent() throws Exception {
      Map<String, Object> progress =
          Map.of(
              "quests",
              List.of(),
              "doneCount",
              1,
              "totalQuests",
              5,
              "allDone",
              false,
              "claimedVoucher",
              false);
      when(questService.completeQuest(USER_ID, "profile")).thenReturn(progress);

      mockMvc
          .perform(post("/api/v1/quests/complete/{questId}", "profile"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.doneCount").value(1));
    }

    @Test
    @DisplayName("allDone=true once all 5 quests are completed")
    void reportsAllDoneWhenComplete() throws Exception {
      Map<String, Object> progress =
          Map.of(
              "quests",
              List.of(),
              "doneCount",
              5,
              "totalQuests",
              5,
              "allDone",
              true,
              "claimedVoucher",
              false);
      when(questService.completeQuest(USER_ID, "leaderboard")).thenReturn(progress);

      mockMvc
          .perform(post("/api/v1/quests/complete/{questId}", "leaderboard"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.allDone").value(true));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/quests/claim-voucher")
  class ClaimVoucher {

    @Test
    @DisplayName("409 COUPON_ALREADY_USED when already claimed")
    void rejectsWhenAlreadyClaimed() throws Exception {
      when(questService.claimVoucher(USER_ID))
          .thenThrow(
              new AppException(ErrorCode.COUPON_ALREADY_USED, "Bạn đã nhận quà tân thủ trước đó."));

      mockMvc.perform(post("/api/v1/quests/claim-voucher")).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("400 VALIDATION_FAILED when not all quests are completed")
    void rejectsWhenQuestsIncomplete() throws Exception {
      when(questService.claimVoucher(USER_ID))
          .thenThrow(
              new AppException(
                  ErrorCode.VALIDATION_FAILED,
                  "Vui lòng hoàn thành tất cả 5 nhiệm vụ để nhận quà!"));

      mockMvc.perform(post("/api/v1/quests/claim-voucher")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("generates and saves a new voucher when all quests done and not yet claimed")
    void generatesVoucherWhenEligible() throws Exception {
      Map<String, Object> voucherMap = Map.of("code", "MCNEW50", "discountPercent", 50);
      when(questService.claimVoucher(USER_ID)).thenReturn(voucherMap);

      mockMvc
          .perform(post("/api/v1/quests/claim-voucher"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.discountPercent").value(50));
    }
  }
}
