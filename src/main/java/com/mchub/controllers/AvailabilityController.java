package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.Schedule;
import com.mchub.services.AvailabilityService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping
    public ResponseEntity<ApiResponse<Schedule>> createAvailability(@RequestBody Schedule schedule) {
        String mcId = SecurityUtils.getCurrentUserId();
        Schedule saved = availabilityService.createSchedule(mcId, schedule);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Created successfully", saved));
    }

    @GetMapping("/{mcId}")
    public ResponseEntity<ApiResponse<List<Schedule>>> getAvailability(@PathVariable String mcId) {
        List<Schedule> list = availabilityService.getSchedules(mcId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAvailability(@PathVariable String id) {
        String mcId = SecurityUtils.getCurrentUserId();
        availabilityService.deleteSchedule(id, mcId);
        return ResponseEntity.ok(ApiResponse.success("Deleted successfully", null));
    }
}
