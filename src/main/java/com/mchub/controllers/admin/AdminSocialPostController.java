package com.mchub.controllers.admin;

import com.mchub.dto.ApiResponse;
import com.mchub.dto.SaveSocialPostRequest;
import com.mchub.dto.SocialPostDTO;
import com.mchub.services.SocialPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/social-posts")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminSocialPostController {

    private final SocialPostService socialPostService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialPostDTO>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("Social posts retrieved",
                socialPostService.getAllPostsAdmin()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SocialPostDTO>> create(
            @RequestBody SaveSocialPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social post created",
                socialPostService.createPost(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SocialPostDTO>> update(
            @PathVariable String id,
            @RequestBody SaveSocialPostRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Social post updated",
                socialPostService.updatePost(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        socialPostService.deletePost(id);
        return ResponseEntity.ok(ApiResponse.success("Social post deleted", null));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<SocialPostDTO>> toggle(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Social post toggled",
                socialPostService.toggleActive(id)));
    }
}
