package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.services.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
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
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.fail("Failed to upload file: " + e.getMessage()));
        }
    }
}
