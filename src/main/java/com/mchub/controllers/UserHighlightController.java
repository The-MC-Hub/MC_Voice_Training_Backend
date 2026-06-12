package com.mchub.controllers;

import com.mchub.models.UserHighlight;
import com.mchub.repositories.UserHighlightRepository;
import com.mchub.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/v1/highlights")
public class UserHighlightController {

    @Autowired
    private UserHighlightRepository highlightRepository;

    @GetMapping("/reading-guides/{guideId}/users/{userId}")
    public ResponseEntity<?> getHighlights(
            @PathVariable String guideId,
            @PathVariable String userId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    "Highlights retrieved successfully",
                    highlightRepository.findByUserIdAndReadingGuideIdOrderByCreatedAtDesc(userId, guideId)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createHighlight(@RequestBody UserHighlight highlight) {
        try {
            highlight.setCreatedAt(new Date());
            highlight.setUpdatedAt(new Date());
            UserHighlight saved = highlightRepository.save(highlight);
            return ResponseEntity.ok(ApiResponse.success("Highlight created", saved));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateHighlight(
            @PathVariable String id,
            @RequestBody UserHighlight request) {
        try {
            return highlightRepository.findById(id).map(highlight -> {
                if (request.getColorHex() != null) highlight.setColorHex(request.getColorHex());
                if (request.getNoteContent() != null) highlight.setNoteContent(request.getNoteContent());
                highlight.setUpdatedAt(new Date());
                return ResponseEntity.ok(ApiResponse.success("Highlight updated", highlightRepository.save(highlight)));
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHighlight(@PathVariable String id) {
        try {
            highlightRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Highlight deleted", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.fail(e.getMessage()));
        }
    }
}
