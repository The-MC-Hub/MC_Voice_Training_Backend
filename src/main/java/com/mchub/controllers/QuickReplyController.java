package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.exception.ErrorCode;
import com.mchub.exception.AppException;
import com.mchub.models.QuickReply;
import com.mchub.repositories.QuickReplyRepository;
import com.mchub.util.SecurityUtils;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/mcs/quick-replies")
public class QuickReplyController {

  private final QuickReplyRepository quickReplyRepository;

  public QuickReplyController(QuickReplyRepository quickReplyRepository) {
    this.quickReplyRepository = quickReplyRepository;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<QuickReply>>> getMyQuickReplies() {
    String currentUserId = SecurityUtils.getCurrentUserId();
    List<QuickReply> replies = quickReplyRepository.findByMcUserIdOrderByDisplayOrderAsc(currentUserId);
    return ResponseEntity.ok(ApiResponse.success("Quick replies fetched successfully", replies));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<QuickReply>> createQuickReply(@RequestBody QuickReply request) {
    String currentUserId = SecurityUtils.getCurrentUserId();
    if (request.getTitle() == null || request.getMessageTemplate() == null) {
      throw new AppException(ErrorCode.VALIDATION_FAILED, "Title and message template are required");
    }

    QuickReply reply = new QuickReply(
        currentUserId,
        request.getTitle(),
        request.getMessageTemplate(),
        request.getDisplayOrder()
    );
    QuickReply saved = quickReplyRepository.save(reply);
    return ResponseEntity.ok(ApiResponse.success("Quick reply created successfully", saved));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteQuickReply(@PathVariable String id) {
    String currentUserId = SecurityUtils.getCurrentUserId();
    QuickReply reply = quickReplyRepository.findById(id)
        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Quick reply not found"));

    if (!reply.getMcUserId().equals(currentUserId)) {
      throw new AppException(ErrorCode.ACCESS_DENIED, "You cannot delete this quick reply");
    }

    quickReplyRepository.delete(reply);
    return ResponseEntity.ok(ApiResponse.success("Quick reply deleted successfully", null));
  }
}
