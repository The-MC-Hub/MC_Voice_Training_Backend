package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.Competition;
import com.mchub.repositories.CompetitionRepository;
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

    private final CompetitionRepository competitionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Competition>>> getAllCompetitions() {
        return ResponseEntity.ok(ApiResponse.success(competitionRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Competition>> createCompetition(@RequestBody Competition competition) {
        if (competition.getStartDate() == null) {
            competition.setStartDate(java.time.Instant.now());
        }
        if (competition.getEndDate() == null) {
            // Default 7 days
            competition.setEndDate(java.time.Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS));
        }
        Competition saved = competitionRepository.save(competition);
        return ResponseEntity.ok(ApiResponse.success("Competition created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Competition>> updateCompetition(
            @PathVariable String id,
            @RequestBody Competition competitionDetails) {
        
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Competition not found with id: " + id));
        
        competition.setTitle(competitionDetails.getTitle());
        competition.setDescription(competitionDetails.getDescription());
        competition.setType(competitionDetails.getType());
        competition.setChallengeScriptId(competitionDetails.getChallengeScriptId());
        competition.setStartDate(competitionDetails.getStartDate());
        competition.setEndDate(competitionDetails.getEndDate());
        competition.setActive(competitionDetails.isActive());
        
        Competition updated = competitionRepository.save(competition);
        return ResponseEntity.ok(ApiResponse.success("Competition updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCompetition(@PathVariable String id) {
        competitionRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Competition deleted successfully", null));
    }
}
