package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.models.SystemSetting;
import com.mchub.repositories.SystemSettingRepository;
import com.mchub.services.SystemHealthService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/system")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminSystemController {

  private final SystemHealthService systemHealthService;
  private final SystemSettingRepository systemSettingRepo;

  @GetMapping("/health")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
    return ResponseEntity.ok(ApiResponse.success(systemHealthService.getSystemHealth()));
  }

  @GetMapping("/settings/dynamic")
  public ResponseEntity<ApiResponse<List<SystemSetting>>> getDynamicSettings() {
    return ResponseEntity.ok(ApiResponse.success(systemSettingRepo.findAll()));
  }

  @PutMapping("/settings/dynamic")
  public ResponseEntity<ApiResponse<Void>> updateDynamicSetting(
      @RequestBody Map<String, String> body) {
    String key = body.get("key");
    String value = body.get("value");
    if (key == null || key.isBlank() || value == null) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.fail("Key and value are required"));
    }
    SystemSetting setting = systemSettingRepo.findById(key).orElse(new SystemSetting());
    setting.setKey(key);
    setting.setValue(value);
    systemSettingRepo.save(setting);
    return ResponseEntity.ok(ApiResponse.success("Setting updated successfully: " + key, null));
  }
}
