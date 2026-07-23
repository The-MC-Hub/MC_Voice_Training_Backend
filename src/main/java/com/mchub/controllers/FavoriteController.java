package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.services.FavoriteService;
import com.mchub.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{mcUserId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(@PathVariable String mcUserId) {
        String clientId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = favoriteService.toggle(clientId, mcUserId);
        return ResponseEntity.ok(ApiResponse.success(
                Boolean.TRUE.equals(result.get("favorited")) ? "Added to favorites" : "Removed from favorites",
                result));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<String>>> getMyFavorites() {
        String clientId = SecurityUtils.getCurrentUserId();
        List<String> favoriteIds = favoriteService.getMyFavoriteIds(clientId);
        return ResponseEntity.ok(ApiResponse.success(favoriteIds));
    }

    @GetMapping("/check/{mcUserId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFavorite(@PathVariable String mcUserId) {
        String clientId = SecurityUtils.getCurrentUserId();
        boolean isFav = favoriteService.check(clientId, mcUserId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("isFavorited", isFav)));
    }
}
