package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.services.MediaService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "mchub_chat") String folder) {
        try {
            String url = mediaService.uploadFile(file, folder);
            return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
        } catch (Exception e) {
            log.error("Media upload failed", e);
            return ResponseEntity.status(500).body(ApiResponse.fail("Failed to upload file"));
        }
    }
}
