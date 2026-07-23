package com.mchub.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.PracticeReview;
import com.mchub.models.PracticeSession;
import com.mchub.models.User;
import com.mchub.repositories.PracticeReviewRepository;
import com.mchub.repositories.PracticeSessionRepository;
import com.mchub.repositories.UserRepository;
import com.mchub.repositories.VoiceLessonRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @WebMvcTest for PeerReviewController. Covers the ownership check on
 * requestReview() (learner can only request review for their own session),
 * the duplicate-request guard, and the submit()/toDTO() enrichment path.
 */
@WebMvcTest(controllers = PeerReviewController.class)
@ContextConfiguration(classes = {PeerReviewController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class PeerReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PracticeReviewRepository practiceReviewRepository;
    @MockBean private PracticeSessionRepository practiceSessionRepository;
    @MockBean private VoiceLessonRepository lessonRepository;
    @MockBean private UserRepository userRepository;

    private static final String USER_ID = "user-review-001";
    private static final String OTHER_USER_ID = "user-review-002";
    private static final String SESSION_ID = "session-1";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("POST /api/v1/peer-review/request/{practiceSessionId}")
    class RequestReview {

        @Test
        @DisplayName("404 when practice session does not exist")
        void notFoundWhenSessionMissing() throws Exception {
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/peer-review/request/{id}", SESSION_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 ACCESS_DENIED when the session belongs to a different user")
        void rejectsWhenNotOwner() throws Exception {
            PracticeSession session = PracticeSession.builder().id(SESSION_ID).userId(OTHER_USER_ID).build();
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

            mockMvc.perform(post("/api/v1/peer-review/request/{id}", SESSION_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when a review was already requested for this session")
        void rejectsDuplicateRequest() throws Exception {
            PracticeSession session = PracticeSession.builder().id(SESSION_ID).userId(USER_ID).build();
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(practiceReviewRepository.existsByPracticeSessionId(SESSION_ID)).thenReturn(true);

            mockMvc.perform(post("/api/v1/peer-review/request/{id}", SESSION_ID))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("creates a PENDING review when owner requests for the first time")
        void createsReviewWhenEligible() throws Exception {
            PracticeSession session = PracticeSession.builder().id(SESSION_ID).userId(USER_ID).lessonId("lesson-1").build();
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
            when(practiceReviewRepository.existsByPracticeSessionId(SESSION_ID)).thenReturn(false);
            when(practiceReviewRepository.save(any(PracticeReview.class)))
                    .thenAnswer(inv -> {
                        PracticeReview r = inv.getArgument(0);
                        r.setId("review-1");
                        return r;
                    });

            mockMvc.perform(post("/api/v1/peer-review/request/{id}", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.revieweeId").value(USER_ID));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/peer-review/{id}/submit")
    class SubmitReview {

        @Test
        @DisplayName("404 when review does not exist")
        void notFoundWhenReviewMissing() throws Exception {
            when(practiceReviewRepository.findById("review-1")).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/peer-review/{id}/submit", "review-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmitBody("Great job", 5))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when review is already REVIEWED")
        void rejectsWhenAlreadyReviewed() throws Exception {
            PracticeReview review = PracticeReview.builder().id("review-1").status("REVIEWED").build();
            when(practiceReviewRepository.findById("review-1")).thenReturn(Optional.of(review));

            mockMvc.perform(post("/api/v1/peer-review/{id}/submit", "review-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmitBody("Great job", 5))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("marks review REVIEWED with reviewer id, comment, and rating on success")
        void submitsReviewSuccessfully() throws Exception {
            PracticeReview review = PracticeReview.builder()
                    .id("review-1").practiceSessionId(SESSION_ID).revieweeId(OTHER_USER_ID).status("PENDING").build();
            when(practiceReviewRepository.findById("review-1")).thenReturn(Optional.of(review));
            when(practiceReviewRepository.save(any(PracticeReview.class))).thenAnswer(inv -> inv.getArgument(0));
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/v1/peer-review/{id}/submit", "review-1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SubmitBody("Great job", 5))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REVIEWED"))
                    .andExpect(jsonPath("$.data.rating").value(5))
                    .andExpect(jsonPath("$.data.comment").value("Great job"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/peer-review/session/{practiceSessionId}")
    class ForSession {

        @Test
        @DisplayName("returns null data when no review exists for the session")
        void returnsNullWhenNoReview() throws Exception {
            when(practiceReviewRepository.findByPracticeSessionId(SESSION_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/peer-review/session/{id}", SESSION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/peer-review/pending")
    class Pending {

        @Test
        @DisplayName("returns enriched DTOs for all PENDING reviews")
        void returnsPendingReviews() throws Exception {
            PracticeReview review = PracticeReview.builder().id("review-1").practiceSessionId(SESSION_ID).revieweeId(USER_ID).status("PENDING").build();
            when(practiceReviewRepository.findByStatus("PENDING")).thenReturn(List.of(review));
            when(practiceSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(User.builder().id(USER_ID).name("Learner A").build()));

            mockMvc.perform(get("/api/v1/peer-review/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].revieweeName").value("Learner A"));
        }
    }

    /** Minimal local mirror of SubmitReviewRequest for JSON serialization in tests. */
    private record SubmitBody(String comment, int rating) {}
}
