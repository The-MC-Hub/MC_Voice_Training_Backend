package com.mchub.controllers;

import com.mchub.exception.GlobalExceptionHandler;
import com.mchub.models.UserVoucher;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VoucherController.class)
@ContextConfiguration(classes = {VoucherController.class, GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class VoucherControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private UserVoucherRepository userVoucherRepository;

    private static final String USER_ID = "user-voucher-001";

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
    @DisplayName("GET /api/v1/vouchers/my")
    class GetMyVouchers {

        @Test
        @DisplayName("200 OK, returns all vouchers for caller (active + used)")
        void returnsAllVouchers() throws Exception {
            when(userVoucherRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/vouchers/my")).andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/vouchers/my/available")
    class GetAvailableVouchers {

        @Test
        @DisplayName("filters out expired vouchers")
        void filtersOutExpired() throws Exception {
            UserVoucher expired = UserVoucher.builder().userId(USER_ID)
                    .expiresAt(LocalDateTime.now().minusDays(1)).build();
            UserVoucher valid = UserVoucher.builder().userId(USER_ID)
                    .expiresAt(LocalDateTime.now().plusDays(1)).build();
            when(userVoucherRepository.findByUserIdAndUsedAtIsNullAndActiveTrue(USER_ID))
                    .thenReturn(List.of(expired, valid));

            mockMvc.perform(get("/api/v1/vouchers/my/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }

        @Test
        @DisplayName("includes vouchers with no expiry date (expiresAt=null)")
        void includesVouchersWithNoExpiry() throws Exception {
            UserVoucher noExpiry = UserVoucher.builder().userId(USER_ID).expiresAt(null).build();
            when(userVoucherRepository.findByUserIdAndUsedAtIsNullAndActiveTrue(USER_ID))
                    .thenReturn(List.of(noExpiry));

            mockMvc.perform(get("/api/v1/vouchers/my/available"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }
}
