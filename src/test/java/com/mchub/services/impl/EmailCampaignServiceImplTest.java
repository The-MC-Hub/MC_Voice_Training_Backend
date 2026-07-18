package com.mchub.services.impl;

import com.mchub.dto.EmailCampaignResponseDTO;
import com.mchub.dto.UserPreviewDTO;
import com.mchub.enums.SubscriptionPlan;
import com.mchub.enums.UserRole;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.EmailCampaign;
import com.mchub.models.EmailTemplate;
import com.mchub.models.User;
import com.mchub.repositories.EmailCampaignRepository;
import com.mchub.repositories.EmailLogRepository;
import com.mchub.repositories.EmailTemplateRepository;
import com.mchub.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EmailCampaignServiceImpl. Covers recipient resolution
 * (PLAN/ROLE/PREMIUM/CUSTOM/ALL targeting + case-insensitive email
 * deduplication) and template/campaign CRUD. The actual async send loop
 * (processCampaignAsync) is not exercised end-to-end here — it involves
 * Thread.sleep(2000) per recipient and real MimeMessage construction,
 * better suited to an integration/manual test (already covered in UC-09
 * manual system testing).
 */
@ExtendWith(MockitoExtension.class)
class EmailCampaignServiceImplTest {

    @Mock private EmailTemplateRepository templateRepository;
    @Mock private EmailCampaignRepository campaignRepository;
    @Mock private EmailLogRepository logRepository;
    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;

    private EmailCampaignServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailCampaignServiceImpl(templateRepository, campaignRepository, logRepository, userRepository, mailSender);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@mchub.vn");
    }

    private User user(String id, String email, UserRole role, SubscriptionPlan plan, boolean premium) {
        return User.builder().id(id).email(email).name("User " + id).role(role).plan(plan).isPremium(premium).build();
    }

    @Nested
    @DisplayName("resolveRecipients (via countRecipients/previewRecipients)")
    class ResolveRecipients {

        @Test
        @DisplayName("PLAN targeting: resolves by SubscriptionPlan list")
        void resolvesByPlan() {
            when(userRepository.findByPlanIn(List.of(SubscriptionPlan.BASIC, SubscriptionPlan.FULL)))
                    .thenReturn(List.of(user("u1", "a@test.local", UserRole.CLIENT, SubscriptionPlan.BASIC, true)));

            int count = service.countRecipients("PLAN", List.of("BASIC", "FULL"), null, null);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("ROLE targeting: resolves by UserRole list")
        void resolvesByRole() {
            when(userRepository.findByRoleIn(List.of(UserRole.MC)))
                    .thenReturn(List.of(user("u1", "mc@test.local", UserRole.MC, SubscriptionPlan.FREE, false)));

            int count = service.countRecipients("ROLE", null, List.of("MC"), null);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("PREMIUM targeting: resolves only isPremium=true users")
        void resolvesByPremium() {
            when(userRepository.findByIsPremiumTrue())
                    .thenReturn(List.of(user("u1", "premium@test.local", UserRole.CLIENT, SubscriptionPlan.FULL, true)));

            int count = service.countRecipients("PREMIUM", null, null, null);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("CUSTOM targeting: resolves by explicit email list")
        void resolvesByCustomEmails() {
            when(userRepository.findByEmailIn(List.of("a@test.local")))
                    .thenReturn(List.of(user("u1", "a@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false)));

            int count = service.countRecipients("CUSTOM", null, null, List.of("a@test.local"));

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("unrecognized/ALL targeting: falls back to findAll")
        void fallsBackToFindAll() {
            when(userRepository.findAll())
                    .thenReturn(List.of(user("u1", "a@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false)));

            int count = service.countRecipients("ALL", null, null, null);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("deduplicates recipients by case-insensitive email")
        void deduplicatesByEmailCaseInsensitive() {
            when(userRepository.findAll()).thenReturn(List.of(
                    user("u1", "Same@Test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false),
                    user("u2", "same@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false),
                    user("u3", "different@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false)));

            int count = service.countRecipients("ALL", null, null, null);

            assertThat(count).isEqualTo(2); // "same@test.local" deduplicated, "different@..." kept
        }

        @Test
        @DisplayName("excludes users with a null email")
        void excludesNullEmails() {
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id("u1").email(null).role(UserRole.CLIENT).build(),
                    user("u2", "valid@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false)));

            int count = service.countRecipients("ALL", null, null, null);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("previewRecipients maps to UserPreviewDTO with plan/role names")
        void previewMapsToDto() {
            when(userRepository.findAll())
                    .thenReturn(List.of(user("u1", "a@test.local", UserRole.MC, SubscriptionPlan.FULL, true)));

            List<UserPreviewDTO> result = service.previewRecipients("ALL", null, null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPlan()).isEqualTo("FULL");
            assertThat(result.get(0).getRole()).isEqualTo("MC");
            assertThat(result.get(0).isPremium()).isTrue();
        }
    }

    @Nested
    @DisplayName("sendCampaign")
    class SendCampaign {

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND when templateId does not exist")
        void throwsWhenTemplateMissing() {
            when(templateRepository.existsById("missing-template")).thenReturn(false);

            assertThatThrownBy(() -> service.sendCampaign("missing-template", "Subject", "ALL", null, null, null))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("creates a PENDING campaign with the resolved recipient count")
        void createsPendingCampaign() {
            when(templateRepository.existsById("tpl-1")).thenReturn(true);
            when(userRepository.findAll()).thenReturn(List.of(
                    user("u1", "a@test.local", UserRole.CLIENT, SubscriptionPlan.FREE, false)));
            when(campaignRepository.save(any(EmailCampaign.class))).thenAnswer(inv -> {
                EmailCampaign c = inv.getArgument(0);
                c.setId("campaign-1");
                return c;
            });
            // processCampaignAsync runs synchronously here (no Spring proxy in unit test);
            // template lookup returns empty so it exits early via the FAILED branch.
            when(campaignRepository.findById("campaign-1")).thenReturn(Optional.empty());

            EmailCampaignResponseDTO result = service.sendCampaign("tpl-1", "Subject", "ALL", null, null, null);

            assertThat(result.getStatus()).isEqualTo("PENDING");
            assertThat(result.getTotalRecipients()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("template CRUD")
    class TemplateCrud {

        @Test
        @DisplayName("createTemplate uses supplied htmlContent when non-blank")
        void usesSuppliedHtmlContent() {
            when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> {
                EmailTemplate t = inv.getArgument(0);
                t.setId("tpl-new");
                return t;
            });

            var result = service.createTemplate("Welcome", "Hi", "<p>custom html</p>", null);

            assertThat(result.getHtmlContent()).isEqualTo("<p>custom html</p>");
        }

        @Test
        @DisplayName("createTemplate generates HTML from designData when htmlContent is blank")
        void generatesHtmlFromDesignWhenBlank() {
            when(templateRepository.save(any(EmailTemplate.class))).thenAnswer(inv -> {
                EmailTemplate t = inv.getArgument(0);
                t.setId("tpl-new");
                return t;
            });
            EmailTemplate.DesignData design = EmailTemplate.DesignData.builder().title("Hello").build();

            var result = service.createTemplate("Welcome", "Hi", "", design);

            assertThat(result.getHtmlContent()).contains("Hello");
            assertThat(result.getHtmlContent()).contains("<!DOCTYPE html>");
        }

        @Test
        @DisplayName("deleteTemplate throws RESOURCE_NOT_FOUND for unknown id")
        void deleteThrowsWhenMissing() {
            when(templateRepository.existsById("missing")).thenReturn(false);

            assertThatThrownBy(() -> service.deleteTemplate("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getCampaignById")
    class GetCampaignById {

        @Test
        @DisplayName("throws RESOURCE_NOT_FOUND for unknown id")
        void throwsWhenMissing() {
            when(campaignRepository.findById("missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCampaignById("missing"))
                    .isInstanceOf(AppException.class)
                    .extracting(ex -> ((AppException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
