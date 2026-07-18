package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.DiscountCode;
import com.mchub.models.User;
import com.mchub.repositories.DiscountCodeRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.UserVoucherRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = QuestController.class)
@ContextConfiguration(classes = {QuestController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class QuestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserRepository userRepository;
    @MockBean private DiscountCodeRepository discountCodeRepository;
    @MockBean private UserVoucherRepository userVoucherRepository;

    private static final String USER_ID = "user-quest-001";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User.UserBuilder baseUser() {
        return User.builder().id(USER_ID).completedQuests(new HashSet<>()).newbieVoucherClaimed(false);
    }

    @Nested
    @DisplayName("GET /api/v1/quests/progress")
    class GetProgress {

        @Test
        @DisplayName("reports doneCount/allDone correctly for partial completion")
        void reportsPartialCompletion() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile", "practice"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            mockMvc.perform(get("/api/v1/quests/progress"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.doneCount").value(2))
                    .andExpect(jsonPath("$.data.totalQuests").value(4))
                    .andExpect(jsonPath("$.data.allDone").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/quests/complete/{questId}")
    class CompleteQuest {

        @Test
        @DisplayName("400 VALIDATION_FAILED for an unknown quest id")
        void rejectsUnknownQuestId() throws Exception {
            mockMvc.perform(post("/api/v1/quests/complete/{questId}", "bogus-quest"))
                    .andExpect(status().isBadRequest());

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("idempotent — completing the same quest twice does not duplicate in the set")
        void isIdempotent() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/quests/complete/{questId}", "profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.doneCount").value(1));
        }

        @Test
        @DisplayName("allDone=true once all 4 quests are completed")
        void reportsAllDoneWhenComplete() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile", "practice", "courses"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/quests/complete/{questId}", "leaderboard"))
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
            User user = baseUser().newbieVoucherClaimed(true).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            mockMvc.perform(post("/api/v1/quests/claim-voucher")).andExpect(status().isConflict());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when not all quests are completed")
        void rejectsWhenQuestsIncomplete() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

            mockMvc.perform(post("/api/v1/quests/claim-voucher")).andExpect(status().isBadRequest());

            verify(discountCodeRepository, never()).save(any(DiscountCode.class));
        }

        @Test
        @DisplayName("generates and saves a new voucher when all quests done and not yet claimed")
        void generatesVoucherWhenEligible() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile", "practice", "courses", "leaderboard"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(discountCodeRepository.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/quests/claim-voucher"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.discountPercent").value(50));

            verify(discountCodeRepository).save(any(DiscountCode.class));
            verify(userVoucherRepository).save(any());
        }

        @Test
        @DisplayName("does not duplicate DiscountCode when it already exists (retry-safe)")
        void doesNotDuplicateExistingCode() throws Exception {
            User user = baseUser().completedQuests(new HashSet<>(Set.of("profile", "practice", "courses", "leaderboard"))).build();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(discountCodeRepository.findByCodeIgnoreCase(any()))
                    .thenReturn(Optional.of(DiscountCode.builder().id("existing").build()));
            when(userVoucherRepository.existsByUserIdAndCode(any(), any())).thenReturn(true);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(post("/api/v1/quests/claim-voucher")).andExpect(status().isOk());

            verify(discountCodeRepository, never()).save(any(DiscountCode.class));
            verify(userVoucherRepository, never()).save(any());
        }
    }
}
