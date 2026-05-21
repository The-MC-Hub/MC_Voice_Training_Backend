package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.EnumOptionDTO;
import com.mchub.dto.MCProfileResponseDTO;
import com.mchub.dto.MCTrainingStatsDTO;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.services.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

        @GetMapping("/landing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLandingData() {
        Map<String, Object> data = publicService.getLandingData();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

        @GetMapping("/featured-training")
    public ResponseEntity<ApiResponse<List<MCTrainingStatsDTO>>> getFeaturedTrainingStats() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getFeaturedMCTrainingStats()));
    }

        @GetMapping("/mcs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> discoverMCs(
            @RequestParam(required = false) String category) {
        List<MCProfileResponseDTO> list = publicService.discoverMCs(category);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "mcs",     list,
            "results", list.size()
        )));
    }

        @GetMapping("/mcs/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMCProfile(@PathVariable String id) {
        MCProfileResponseDTO profile = publicService.getMCProfile(Objects.requireNonNull(id));
        if (profile == null) {
            throw new AppException(ErrorCode.MC_PROFILE_NOT_FOUND);
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("profile", profile)));
    }

    



    @GetMapping("/enums/user-roles")
    public ResponseEntity<ApiResponse<List<EnumOptionDTO>>> getUserRoles() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getUserRoles()));
    }





    @GetMapping("/enums/report-reasons")
    public ResponseEntity<ApiResponse<List<EnumOptionDTO>>> getReportReasons() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getReportReasons()));
    }
}
