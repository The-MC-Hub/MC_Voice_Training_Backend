package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.exception.AppException;
import com.mchub.exception.ErrorCode;
import com.mchub.models.CaseStudy;
import com.mchub.repositories.CaseStudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/case-studies")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminCaseStudyController {

    private final CaseStudyRepository caseStudyRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaseStudy>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("Case studies retrieved", caseStudyRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStudy>> getOne(@PathVariable String id) {
        CaseStudy caseStudy = caseStudyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Case study not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Case study retrieved", caseStudy));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CaseStudy>> create(@RequestBody CaseStudy request) {
        request.setId(null);
        return ResponseEntity.ok(ApiResponse.success("Case study created", caseStudyRepository.save(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CaseStudy>> update(@PathVariable String id, @RequestBody CaseStudy request) {
        caseStudyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND, "Case study not found: " + id));
        request.setId(id);
        return ResponseEntity.ok(ApiResponse.success("Case study updated", caseStudyRepository.save(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        caseStudyRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Case study deleted", null));
    }
}
