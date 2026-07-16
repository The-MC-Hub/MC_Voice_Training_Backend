package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.CourseResponseDTO;
import com.mchub.dto.SaveCourseRequest;
import com.mchub.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/courses")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;

    /** GET /api/v1/admin/courses — all courses with enrollment stats */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponseDTO>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("All courses retrieved",
                courseService.getAllCoursesAdmin()));
    }

    /** GET /api/v1/admin/courses/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> getOne(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Course retrieved",
                courseService.getCourseDetail(id, null)));
    }

    /** POST /api/v1/admin/courses */
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponseDTO>> create(
            @Valid @RequestBody SaveCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course created",
                courseService.createCourse(request)));
    }

    /** PUT /api/v1/admin/courses/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> update(
            @PathVariable String id,
            @Valid @RequestBody SaveCourseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Course updated",
                courseService.updateCourse(id, request)));
    }

    /** PATCH /api/v1/admin/courses/{id}/pricing — set price & discount for single-course purchase */
    @PatchMapping("/{id}/pricing")
    public ResponseEntity<ApiResponse<CourseResponseDTO>> updatePricing(
            @PathVariable String id,
            @RequestParam(required = false) Integer priceVnd,
            @RequestParam(required = false) Integer discountPercent) {
        return ResponseEntity.ok(ApiResponse.success("Course pricing updated",
                courseService.updatePricing(id, priceVnd, discountPercent)));
    }

    /** DELETE /api/v1/admin/courses/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted", null));
    }
}
