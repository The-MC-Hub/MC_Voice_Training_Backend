package com.mchub.controllers;

import com.mchub.dto.ApiResponse;
import com.mchub.enums.ErrorCode;
import com.mchub.exception.AppException;
import com.mchub.models.ScriptDocument;
import com.mchub.models.ScriptRevision;
import com.mchub.repositories.ScriptDocumentRepository;
import com.mchub.repositories.ScriptRevisionRepository;
import com.mchub.util.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scripts")
public class ScriptCollaborativeController {

  private final ScriptDocumentRepository scriptRepository;
  private final ScriptRevisionRepository revisionRepository;

  public ScriptCollaborativeController(
      ScriptDocumentRepository scriptRepository, ScriptRevisionRepository revisionRepository) {
    this.scriptRepository = scriptRepository;
    this.revisionRepository = revisionRepository;
  }

  @GetMapping("/booking/{bookingId}")
  public ResponseEntity<ApiResponse<ScriptDocument>> getScriptByBooking(@PathVariable String bookingId) {
    ScriptDocument doc =
        scriptRepository
            .findByBookingId(bookingId)
            .orElseGet(
                () -> {
                  ScriptDocument newDoc = new ScriptDocument();
                  newDoc.setBookingId(bookingId);
                  newDoc.setTitle("Event Script #" + bookingId);
                  newDoc.setContent("");
                  return scriptRepository.save(newDoc);
                });

    return ResponseEntity.ok(ApiResponse.success("Script retrieved successfully", doc));
  }

  @PostMapping("/booking/{bookingId}/annotations")
  public ResponseEntity<ApiResponse<ScriptDocument>> addAnnotation(
      @PathVariable String bookingId, @RequestBody ScriptDocument.Annotation annotation) {
    String currentUserId = SecurityUtils.getCurrentUserId();
    ScriptDocument doc =
        scriptRepository
            .findByBookingId(bookingId)
            .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Script not found"));

    annotation.setAuthorUserId(currentUserId);
    doc.getAnnotations().add(annotation);
    doc.setUpdatedAt(LocalDateTime.now());
    ScriptDocument updated = scriptRepository.save(doc);

    return ResponseEntity.ok(ApiResponse.success("Annotation added successfully", updated));
  }

  @GetMapping("/booking/{bookingId}/revisions")
  public ResponseEntity<ApiResponse<List<ScriptRevision>>> getRevisions(@PathVariable String bookingId) {
    List<ScriptRevision> revisions =
        revisionRepository.findByBookingIdOrderByRevisionNumberDesc(bookingId);
    return ResponseEntity.ok(ApiResponse.success("Revisions retrieved successfully", revisions));
  }

  @MessageMapping("/script.edit/{bookingId}")
  @SendTo("/topic/script/{bookingId}")
  public ScriptDocument handleRealtimeScriptEdit(
      @DestinationVariable String bookingId, ScriptDocument editPayload) {
    ScriptDocument doc =
        scriptRepository
            .findByBookingId(bookingId)
            .orElseGet(
                () -> {
                  ScriptDocument n = new ScriptDocument();
                  n.setBookingId(bookingId);
                  return n;
                });

    doc.setContent(editPayload.getContent());
    doc.setLastEditedByUserId(editPayload.getLastEditedByUserId());
    doc.setUpdatedAt(LocalDateTime.now());
    ScriptDocument saved = scriptRepository.save(doc);

    // Save revision snapshot
    List<ScriptRevision> existing = revisionRepository.findByBookingIdOrderByRevisionNumberDesc(bookingId);
    int nextRevNum = existing.isEmpty() ? 1 : existing.get(0).getRevisionNumber() + 1;
    ScriptRevision rev = new ScriptRevision(saved.getId(), bookingId, editPayload.getLastEditedByUserId(), saved.getContent(), nextRevNum);
    revisionRepository.save(rev);

    return saved;
  }
}
