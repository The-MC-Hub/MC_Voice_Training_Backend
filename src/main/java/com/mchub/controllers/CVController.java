package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.models.CVDocument;
import com.mchub.repositories.CVDocumentRepository;
import com.mchub.services.SupabaseStorageService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cv")
@RequiredArgsConstructor
@Slf4j
public class CVController {

    private final SupabaseStorageService supabaseStorageService;
    private final CVDocumentRepository cvDocumentRepository;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<CVDocument>> uploadCV(@RequestParam("file") MultipartFile file) {
        String userId = SecurityUtils.getCurrentUserId();
        try {
            String url = supabaseStorageService.uploadCV(file, userId);
            CVDocument doc = CVDocument.builder()
                    .userId(userId)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(url)
                    .fileSizeBytes(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .build();
            return ResponseEntity.ok(ApiResponse.success(cvDocumentRepository.save(doc)));
        } catch (Exception e) {
            log.error("CV upload failed", e);
            return ResponseEntity.status(500).body(ApiResponse.fail(SecurityUtils.safeMessage(e)));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CVDocument>>> listMyCVs() {
        String userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                cvDocumentRepository.findByUserIdOrderByUploadedAtDesc(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCV(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        CVDocument doc = cvDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("CV not found with id: " + id));
        if (!doc.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResponse.fail("Not your document"));
        }
        cvDocumentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
