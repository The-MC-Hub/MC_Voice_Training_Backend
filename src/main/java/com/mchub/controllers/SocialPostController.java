package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.SocialPostDTO;
import com.mchub.services.SocialPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/social-posts")
@RequiredArgsConstructor
public class SocialPostController {

    private final SocialPostService socialPostService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialPostDTO>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Active social posts",
                socialPostService.getActivePosts()));
    }

    @PostMapping("/{id}/click")
    public ResponseEntity<ApiResponse<Void>> recordClick(@PathVariable String id) {
        socialPostService.recordClick(id);
        return ResponseEntity.ok(ApiResponse.success("Click recorded", null));
    }
}
