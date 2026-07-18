package com.mchub.controllers;

import com.mchub.dto.PracticeSessionResponseDTO;
import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.GuestVoiceUsage;
import com.mchub.models.SystemSetting;
import com.mchub.repositories.GuestVoiceUsageRepository;
import com.mchub.repositories.SystemSettingRepository;
import com.mchub.services.VoiceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @WebMvcTest for VoiceController. Covers audio-file validation (content-type
 * allowlist, 20MB size cap, magic-byte sniffing), the IDOR guard in
 * getHistory() (manual SecurityContextHolder authority check, not
 * @PreAuthorize), and the guest-cooldown gate on the unauthenticated
 * analyze-guest endpoint.
 */
@WebMvcTest(controllers = VoiceController.class)
@ContextConfiguration(classes = {VoiceController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class VoiceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private VoiceService voiceService;
    @MockBean private GuestVoiceUsageRepository guestUsageRepo;
    @MockBean private SystemSettingRepository systemSettingRepo;

    private static final String USER_ID = "user-voice-001";

    /** Minimal valid WAV: "RIFF" + 4 junk bytes + "WAVE" (12 bytes, passes magic-byte check). */
    private static final byte[] VALID_WAV_BYTES =
            "RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String userId, String... authorities) {
        List<SimpleGrantedAuthority> auths = List.of(authorities).stream()
                .map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, auths));
    }

    @Nested
    @DisplayName("GET /api/v1/voice/lessons — public")
    class GetLessons {

        @Test
        @DisplayName("no filters: returns all lessons")
        void returnsAllLessonsWithNoFilters() throws Exception {
            when(voiceService.getAllLessons()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/voice/lessons")).andExpect(status().isOk());

            verify(voiceService).getAllLessons();
        }

        @Test
        @DisplayName("search param present: delegates to searchLessons")
        void delegatesToSearchWhenSearchPresent() throws Exception {
            when(voiceService.searchLessons(anyString(), any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/voice/lessons").param("search", "wedding"))
                    .andExpect(status().isOk());

            verify(voiceService).searchLessons("wedding", null);
            verify(voiceService, never()).getAllLessons();
        }

        @Test
        @DisplayName("category param present (no search): delegates to getLessonsByCategory")
        void delegatesToCategoryWhenCategoryPresentNoSearch() throws Exception {
            when(voiceService.getLessonsByCategory(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/voice/lessons").param("category", "WEDDING"))
                    .andExpect(status().isOk());

            verify(voiceService, never()).getAllLessons();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/voice/practice/analyze-voice — audio validation")
    class AnalyzePractice {

        @Test
        @DisplayName("400 VALIDATION_FAILED for an unsupported content-type")
        void rejectsUnsupportedContentType() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            MockMultipartFile file = new MockMultipartFile("audioFile", "malware.exe",
                    "application/x-msdownload", VALID_WAV_BYTES);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-voice")
                            .file(file).param("lessonId", "lesson-1"))
                    .andExpect(status().isBadRequest());

            verify(voiceService, never()).analyzePractice(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when file exceeds 20MB")
        void rejectsOversizedFile() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            byte[] oversized = new byte[21 * 1024 * 1024];
            System.arraycopy(VALID_WAV_BYTES, 0, oversized, 0, VALID_WAV_BYTES.length);
            MockMultipartFile file = new MockMultipartFile("audioFile", "big.wav", "audio/wav", oversized);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-voice")
                            .file(file).param("lessonId", "lesson-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 VALIDATION_FAILED when content-type claims audio/wav but magic bytes don't match")
        void rejectsSpoofedContentType() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            MockMultipartFile file = new MockMultipartFile("audioFile", "fake.wav",
                    "audio/wav", "not a real wav file content".getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-voice")
                            .file(file).param("lessonId", "lesson-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("200 OK for a valid WAV file, delegates to voiceService with caller's userId")
        void acceptsValidWavAndDelegates() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.wav", "audio/wav", VALID_WAV_BYTES);
            when(voiceService.analyzePractice("lesson-1", USER_ID, file)).thenReturn(new PracticeSessionResponseDTO());

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-voice")
                            .file(file).param("lessonId", "lesson-1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("accepts content-type with codec suffix (e.g. audio/webm;codecs=opus)")
        void acceptsContentTypeWithCodecSuffix() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            byte[] webmBytes = new byte[]{0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0, 0, 0, 0};
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.webm",
                    "audio/webm;codecs=opus", webmBytes);
            when(voiceService.analyzePractice(anyString(), anyString(), any())).thenReturn(new PracticeSessionResponseDTO());

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-voice")
                            .file(file).param("lessonId", "lesson-1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/voice/practice/history/{userId} — IDOR guard")
    class GetHistory {

        @Test
        @DisplayName("403 ACCESS_DENIED when a CLIENT requests another user's history")
        void rejectsAccessToAnotherUsersHistory() throws Exception {
            authenticateAs(USER_ID, "CLIENT");

            mockMvc.perform(get("/api/v1/voice/practice/history/{userId}", "other-user"))
                    .andExpect(status().isForbidden());

            verify(voiceService, never()).getUserPracticeHistory(anyString());
        }

        @Test
        @DisplayName("200 OK when requesting own history")
        void allowsAccessToOwnHistory() throws Exception {
            authenticateAs(USER_ID, "CLIENT");
            when(voiceService.getUserPracticeHistory(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/voice/practice/history/{userId}", USER_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 OK when an ADMIN requests another user's history")
        void allowsAdminAccessToAnyHistory() throws Exception {
            authenticateAs("admin-001", "ADMIN");
            when(voiceService.getUserPracticeHistory(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/voice/practice/history/{userId}", USER_ID))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/voice/practice/analyze-guest — cooldown gate, no auth required")
    class AnalyzeGuestVoice {

        @Test
        @DisplayName("400 VALIDATION_FAILED when guest is still within cooldown window")
        void rejectsWithinCooldown() throws Exception {
            when(guestUsageRepo.findById(anyString())).thenReturn(Optional.of(
                    GuestVoiceUsage.builder().ipAddress("127.0.0.1").lastUsedAt(LocalDateTime.now().minusHours(1)).build()));
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.wav", "audio/wav", VALID_WAV_BYTES);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-guest").file(file))
                    .andExpect(status().isBadRequest());

            verify(voiceService, never()).proxyAnalyzeVoice(any(), any());
        }

        @Test
        @DisplayName("200 OK when guest has never used the feature before, saves usage record")
        void allowsFirstTimeGuestAndSavesUsage() throws Exception {
            when(guestUsageRepo.findById(anyString())).thenReturn(Optional.empty());
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());
            when(voiceService.proxyAnalyzeVoice(any(), any())).thenReturn(java.util.Map.of("status", "success"));
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.wav", "audio/wav", VALID_WAV_BYTES);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-guest").file(file))
                    .andExpect(status().isOk());

            verify(guestUsageRepo).save(any(GuestVoiceUsage.class));
        }

        @Test
        @DisplayName("200 OK when cooldown has already elapsed")
        void allowsAfterCooldownElapsed() throws Exception {
            when(guestUsageRepo.findById(anyString())).thenReturn(Optional.of(
                    GuestVoiceUsage.builder().ipAddress("127.0.0.1").lastUsedAt(LocalDateTime.now().minusHours(10)).build()));
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());
            when(voiceService.proxyAnalyzeVoice(any(), any())).thenReturn(java.util.Map.of("status", "success"));
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.wav", "audio/wav", VALID_WAV_BYTES);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-guest").file(file))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("uses custom cooldown hours from SystemSetting when present")
        void usesCustomCooldownFromSetting() throws Exception {
            SystemSetting setting = new SystemSetting();
            setting.setKey("GUEST_COOLDOWN_HOURS");
            setting.setValue("1");
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.of(setting));
            // Used 2 hours ago, cooldown is 1 hour -> should be allowed
            when(guestUsageRepo.findById(anyString())).thenReturn(Optional.of(
                    GuestVoiceUsage.builder().ipAddress("127.0.0.1").lastUsedAt(LocalDateTime.now().minusHours(2)).build()));
            when(voiceService.proxyAnalyzeVoice(any(), any())).thenReturn(java.util.Map.of("status", "success"));
            MockMultipartFile file = new MockMultipartFile("audioFile", "test.wav", "audio/wav", VALID_WAV_BYTES);

            mockMvc.perform(multipart("/api/v1/voice/practice/analyze-guest").file(file))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/voice/lessons/{id}/adaptive-stats")
    class GetAdaptiveStats {

        @Test
        @DisplayName("returns null data with a friendly message when no adaptive data exists yet")
        void returnsNullDataWhenNoStats() throws Exception {
            when(voiceService.getAdaptiveStats("lesson-1")).thenReturn(null);

            mockMvc.perform(get("/api/v1/voice/lessons/{id}/adaptive-stats", "lesson-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("No adaptive data yet (need 10+ sessions)"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/voice/guest-cooldown-hours")
    class GetGuestCooldownHours {

        @Test
        @DisplayName("defaults to 3 when no setting is configured")
        void defaultsToThree() throws Exception {
            when(systemSettingRepo.findById("GUEST_COOLDOWN_HOURS")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/voice/guest-cooldown-hours"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hours").value(3));
        }
    }
}
