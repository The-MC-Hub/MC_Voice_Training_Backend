package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.Competition;
import com.mchub.services.CompetitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/competitions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Competition>>> getAllCompetitions() {
        return ResponseEntity.ok(ApiResponse.success(competitionService.getAllCompetitions()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Competition>> createCompetition(@RequestBody Competition competition) {
        Competition saved = competitionService.createCompetition(competition);
        return ResponseEntity.ok(ApiResponse.success("Competition created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Competition>> updateCompetition(
            @PathVariable String id,
            @RequestBody Competition competitionDetails) {
        Competition updated = competitionService.updateCompetition(id, competitionDetails);
        return ResponseEntity.ok(ApiResponse.success("Competition updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompetition(@PathVariable String id) {
        competitionService.deleteCompetition(id);
        return ResponseEntity.ok(ApiResponse.success("Competition deleted successfully", null));
    }
}
