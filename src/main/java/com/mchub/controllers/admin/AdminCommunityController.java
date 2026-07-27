package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.CommunityStatsDTO;
import com.mchub.models.UserHighlight;
import com.mchub.repositories.UserHighlightRepository;
import com.mchub.services.CommunityService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/community")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminCommunityController {

  private final CommunityService communityService;
  private final UserHighlightRepository highlightRepository;

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<CommunityStatsDTO>> getCommunityStats() {
    return ResponseEntity.ok(ApiResponse.success(communityService.getCommunityStats()));
  }

  @GetMapping("/highlights")
  public ResponseEntity<ApiResponse<List<UserHighlight>>> getAllHighlights() {
    return ResponseEntity.ok(ApiResponse.success(highlightRepository.findAll()));
  }

  @DeleteMapping("/highlights/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable String id) {
    highlightRepository.deleteById(id);
    return ResponseEntity.ok(ApiResponse.success("Highlight deleted by admin", null));
  }
}
