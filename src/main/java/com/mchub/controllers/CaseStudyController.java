package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.CaseStudy;
import com.mchub.repositories.CaseStudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/case-studies")
@RequiredArgsConstructor
public class CaseStudyController {

    private final CaseStudyRepository caseStudyRepository;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStudy>> getCaseStudy(@PathVariable String id) {
        CaseStudy caseStudy = caseStudyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Case study not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Case study retrieved", caseStudy));
    }
}
