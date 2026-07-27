package com.mchub.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.mchub.enums.UserRole;
import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSanctionSchedulerTest {

  @Mock private UserRepository userRepository;

  @InjectMocks private UserSanctionScheduler scheduler;

  @Test
  @DisplayName("processExpiredSuspensions auto-reactivates users whose suspendedUntil has passed")
  void processExpiredSuspensionsAutoReactivates() {
    User expiredUser =
        User.builder()
            .id("user-1")
            .role(UserRole.CLIENT)
            .isActive(false)
            .suspendedUntil(LocalDateTime.now().minusMinutes(5))
            .suspendReason("Test temporary ban")
            .sanctionHistory(new ArrayList<>())
            .build();

    when(userRepository.findByRoleNot(UserRole.ADMIN)).thenReturn(List.of(expiredUser));

    scheduler.processExpiredSuspensions();

    assertThat(expiredUser.isActive()).isTrue();
    assertThat(expiredUser.getSuspendedUntil()).isNull();
    assertThat(expiredUser.getSanctionHistory()).hasSize(1);
    assertThat(expiredUser.getSanctionHistory().get(0).getAction()).isEqualTo("AUTO_UNSUSPEND");

    verify(userRepository).save(expiredUser);
  }
}
