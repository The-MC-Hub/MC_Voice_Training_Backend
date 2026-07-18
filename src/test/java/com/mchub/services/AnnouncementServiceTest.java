package com.mchub.services;

import com.mchub.dto.UserResponseDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.Announcement;
import com.mchub.models.User;
import com.mchub.repositories.AnnouncementRepository;
import com.mchub.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnnouncementService. Also documents a known DRY finding
 * from the audit (Remaining_Modules_Audit_Report.md 3.1): the two
 * approveAndSend() overloads duplicate ~35 lines of send logic almost
 * identically. Not fixed here — QA reports, does not fix production code.
 *
 * create() reads the current user via SecurityUtils.getCurrentUserId(), which
 * reads Spring Security's SecurityContextHolder — a real Authentication is
 * populated in setUp()/cleared in tearDown() rather than static-mocking
 * SecurityUtils.
 */
@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepo;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    private AnnouncementService service;

    private static final String ANN_ID = "ann-001";
    private static final String ADMIN_ID = "admin-001";

    @BeforeEach
    void setUp() {
        service = new AnnouncementService(announcementRepo, userRepository, emailService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(ADMIN_ID, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Announcement.AnnouncementBuilder draftAnnouncement() {
        return Announcement.builder().id(ANN_ID).title("Title").content("Hi {{name}}")
                .status(Announcement.AnnouncementStatus.DRAFT);
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("forces status=DRAFT, id=null, sentAt=null, recipientCount=0 regardless of input")
        void forcesDraftDefaults() {
            Announcement input = Announcement.builder().id("client-supplied-id")
                    .status(Announcement.AnnouncementStatus.SENT)
                    .sentAt(java.time.LocalDateTime.now()).recipientCount(999).build();
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            Announcement result = service.create(input);

            assertThat(result.getId()).isNull();
            assertThat(result.getStatus()).isEqualTo(Announcement.AnnouncementStatus.DRAFT);
            assertThat(result.getSentAt()).isNull();
            assertThat(result.getRecipientCount()).isZero();
        }

        @Test
        @DisplayName("sets createdBy from the authenticated user")
        void setsCreatedByFromAuth() {
            Announcement input = Announcement.builder().build();
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            Announcement result = service.create(input);

            assertThat(result.getCreatedBy()).isEqualTo(ADMIN_ID);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("throws VALIDATION_FAILED when trying to edit a SENT announcement")
        void throwsWhenEditingSentAnnouncement() {
            Announcement sent = draftAnnouncement().status(Announcement.AnnouncementStatus.SENT).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(sent));

            assertThatThrownBy(() -> service.update(ANN_ID, Announcement.builder().title("New").build()))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("overwrites content fields for a DRAFT announcement")
        void updatesContentForDraft() {
            Announcement existing = draftAnnouncement().build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(existing));
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            Announcement update = Announcement.builder().title("New Title").content("New content").build();
            Announcement result = service.update(ANN_ID, update);

            assertThat(result.getTitle()).isEqualTo("New Title");
            assertThat(result.getContent()).isEqualTo("New content");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("throws VALIDATION_FAILED when trying to delete a SENT announcement")
        void throwsWhenDeletingSentAnnouncement() {
            Announcement sent = draftAnnouncement().status(Announcement.AnnouncementStatus.SENT).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(sent));

            assertThatThrownBy(() -> service.delete(ANN_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("deletes a DRAFT announcement")
        void deletesDraft() {
            Announcement draft = draftAnnouncement().build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(draft));

            service.delete(ANN_ID);

            org.mockito.Mockito.verify(announcementRepo).deleteById(ANN_ID);
        }
    }

    @Nested
    @DisplayName("getUsersByPlan")
    class GetUsersByPlan {

        @Test
        @DisplayName("filters out inactive users and users without an email")
        void filtersInactiveAndEmailless() {
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email("a@test.local").isActive(true).build(),
                    User.builder().id("u2").email(null).isActive(true).build(),
                    User.builder().id("u3").email("c@test.local").isActive(false).build()));

            List<UserResponseDTO> result = service.getUsersByPlan(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("u1");
        }

        @Test
        @DisplayName("filters by plan (case-insensitive) when a plan filter is given")
        void filtersByPlanCaseInsensitive() {
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email("a@test.local").isActive(true).plan(SubscriptionPlan.BASIC).build(),
                    User.builder().id("u2").email("b@test.local").isActive(true).plan(SubscriptionPlan.FULL).build()));

            List<UserResponseDTO> result = service.getUsersByPlan("basic");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo("u1");
        }
    }

    @Nested
    @DisplayName("approveAndSend(id) — resolveRecipients by targetPlans")
    class ApproveAndSendByTargetPlans {

        @Test
        @DisplayName("throws VALIDATION_FAILED when announcement is already SENT")
        void throwsWhenAlreadySent() {
            Announcement sent = draftAnnouncement().status(Announcement.AnnouncementStatus.SENT).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(sent));

            assertThatThrownBy(() -> service.approveAndSend(ANN_ID))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_FAILED);
        }

        @Test
        @DisplayName("marks status=SENT with recipientCount and sentAt set")
        void marksAsSent() {
            Announcement draft = draftAnnouncement().targetPlans(null).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(draft));
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email("a@test.local").isActive(true).name("Alice").build()));
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(emailService.buildHtmlEmail(any(), any(), any())).thenReturn("<html></html>");

            service.approveAndSend(ANN_ID);

            ArgumentCaptor<Announcement> captor = ArgumentCaptor.forClass(Announcement.class);
            org.mockito.Mockito.verify(announcementRepo).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Announcement.AnnouncementStatus.SENT);
            assertThat(captor.getValue().getSentAt()).isNotNull();
            assertThat(captor.getValue().getRecipientCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("continues sending to remaining recipients even if one email send fails")
        void continuesOnIndividualSendFailure() throws Exception {
            Announcement draft = draftAnnouncement().targetPlans(null).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(draft));
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email("fail@test.local").isActive(true).name("Fail").build(),
                    User.builder().id("u2").email("ok@test.local").isActive(true).name("Ok").build()));
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(emailService.buildHtmlEmail(any(), any(), any())).thenReturn("<html></html>");
            org.mockito.Mockito.doThrow(new RuntimeException("SMTP down"))
                    .when(emailService).sendHtmlEmail(org.mockito.ArgumentMatchers.eq("fail@test.local"), any(), any());

            service.approveAndSend(ANN_ID); // must not throw

            org.mockito.Mockito.verify(emailService).sendHtmlEmail(org.mockito.ArgumentMatchers.eq("ok@test.local"), any(), any());
        }
    }

    @Nested
    @DisplayName("approveAndSend(id, recipientIds) — explicit recipient list overload")
    class ApproveAndSendByExplicitIds {

        @Test
        @DisplayName("explicit recipientIds takes priority over the announcement's saved recipientIds")
        void explicitIdsTakePriority() throws Exception {
            Announcement draft = draftAnnouncement().recipientIds(List.of("u-saved")).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(draft));
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u-explicit").email("explicit@test.local").build(),
                    User.builder().id("u-saved").email("saved@test.local").build()));
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(emailService.buildHtmlEmail(any(), any(), any())).thenReturn("<html></html>");

            service.approveAndSend(ANN_ID, List.of("u-explicit"));

            org.mockito.Mockito.verify(emailService).sendHtmlEmail(
                    org.mockito.ArgumentMatchers.eq("explicit@test.local"), any(), any());
            org.mockito.Mockito.verify(emailService, org.mockito.Mockito.never()).sendHtmlEmail(
                    org.mockito.ArgumentMatchers.eq("saved@test.local"), any(), any());
        }

        @Test
        @DisplayName("falls back to targetPlans resolution when both explicit and saved recipientIds are empty")
        void fallsBackToTargetPlansWhenBothEmpty() throws Exception {
            Announcement draft = draftAnnouncement().recipientIds(null).targetPlans(null).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(draft));
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email("a@test.local").isActive(true).build()));
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));
            when(emailService.buildHtmlEmail(any(), any(), any())).thenReturn("<html></html>");

            service.approveAndSend(ANN_ID, null);

            org.mockito.Mockito.verify(emailService).sendHtmlEmail(
                    org.mockito.ArgumentMatchers.eq("a@test.local"), any(), any());
        }
    }

    @Nested
    @DisplayName("createFromTrigger")
    class CreateFromTrigger {

        @Test
        @DisplayName("builds a DRAFT announcement with recipientCount=0")
        void buildsDraftFromTrigger() {
            when(announcementRepo.save(any(Announcement.class))).thenAnswer(inv -> inv.getArgument(0));

            Announcement result = service.createFromTrigger(
                    Announcement.AnnouncementType.NEW_LESSON, "New Lesson!", "Subject", "Content",
                    "lesson-1", "VOICE_LESSON", null);

            assertThat(result.getStatus()).isEqualTo(Announcement.AnnouncementStatus.DRAFT);
            assertThat(result.getRecipientCount()).isZero();
            assertThat(result.getRefId()).isEqualTo("lesson-1");
        }
    }

    @Nested
    @DisplayName("previewStats")
    class PreviewStats {

        @Test
        @DisplayName("returns 'Tất cả người dùng' when targetPlans is empty")
        void returnsAllUsersLabelWhenTargetPlansEmpty() {
            Announcement ann = draftAnnouncement().targetPlans(List.of()).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(ann));
            when(userRepository.findAll()).thenReturn(List.of());

            Map<String, Object> result = service.previewStats(ANN_ID);

            assertThat(result.get("targetPlans")).isEqualTo("Tất cả người dùng");
        }

        @Test
        @DisplayName("joins targetPlans with comma when non-empty")
        void joinsTargetPlansWithComma() {
            Announcement ann = draftAnnouncement().targetPlans(List.of("BASIC", "FULL")).build();
            when(announcementRepo.findById(ANN_ID)).thenReturn(Optional.of(ann));
            when(userRepository.findAll()).thenReturn(List.of());

            Map<String, Object> result = service.previewStats(ANN_ID);

            assertThat(result.get("targetPlans")).isEqualTo("BASIC, FULL");
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND for unknown id")
        void throwsForUnknownId() {
            when(announcementRepo.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
