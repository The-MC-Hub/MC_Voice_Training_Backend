package com.mchub.services;

import com.mchub.models.User;
import com.mchub.repositories.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSanctionScheduler {

  private final UserRepository userRepository;

  /** Runs every 5 minutes to auto-reactivate users whose temporary suspension has expired. */
  @Scheduled(fixedRate = 300000)
  public void processExpiredSuspensions() {
    LocalDateTime now = LocalDateTime.now();
    List<User> suspendedUsers = userRepository.findByRoleNot(com.mchub.enums.UserRole.ADMIN);
    int reactivatedCount = 0;

    for (User u : suspendedUsers) {
      if (u.getSuspendedUntil() != null && u.getSuspendedUntil().isBefore(now)) {
        u.setActive(true);
        u.setSuspendedUntil(null);
        u.setSuspendReason(null);
        if (u.getSanctionHistory() != null) {
          u.getSanctionHistory().add(
              User.SanctionLog.builder()
                  .action("AUTO_UNSUSPEND")
                  .reason("Temporary ban expired")
                  .timestamp(now)
                  .adminId("SYSTEM")
                  .build());
        }
        userRepository.save(u);
        reactivatedCount++;
      }
    }

    if (reactivatedCount > 0) {
      log.info("🔓 [SANCTION SCHEDULER] Auto-reactivated {} users whose ban expired", reactivatedCount);
    }
  }
}
