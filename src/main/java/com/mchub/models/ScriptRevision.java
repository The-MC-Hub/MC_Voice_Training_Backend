package com.mchub.models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "script_revisions")
public class ScriptRevision {

  @Id private String id;
  private String scriptId;
  private String bookingId;
  private String editedByUserId;
  private String contentSnapshot;
  private int revisionNumber;
  private LocalDateTime createdAt;

  public ScriptRevision() {
    this.createdAt = LocalDateTime.now();
  }

  public ScriptRevision(String scriptId, String bookingId, String editedByUserId, String contentSnapshot, int revisionNumber) {
    this.scriptId = scriptId;
    this.bookingId = bookingId;
    this.editedByUserId = editedByUserId;
    this.contentSnapshot = contentSnapshot;
    this.revisionNumber = revisionNumber;
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getScriptId() {
    return scriptId;
  }

  public void setScriptId(String scriptId) {
    this.scriptId = scriptId;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getEditedByUserId() {
    return editedByUserId;
  }

  public void setEditedByUserId(String editedByUserId) {
    this.editedByUserId = editedByUserId;
  }

  public String getContentSnapshot() {
    return contentSnapshot;
  }

  public void setContentSnapshot(String contentSnapshot) {
    this.contentSnapshot = contentSnapshot;
  }

  public int getRevisionNumber() {
    return revisionNumber;
  }

  public void setRevisionNumber(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
