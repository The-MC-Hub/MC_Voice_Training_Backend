package com.mchub.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "script_documents")
public class ScriptDocument {

  @Id private String id;
  private String bookingId;
  private String title;
  private String content;
  private String lastEditedByUserId;
  private List<Annotation> annotations = new ArrayList<>();
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static class Annotation {
    private int lineNumber;
    private String authorUserId;
    private String note;
    private String type; // PRONUNCIATION, PAUSE, STRESS

    public Annotation() {}

    public Annotation(int lineNumber, String authorUserId, String note, String type) {
      this.lineNumber = lineNumber;
      this.authorUserId = authorUserId;
      this.note = note;
      this.type = type;
    }

    public int getLineNumber() {
      return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
      this.lineNumber = lineNumber;
    }

    public String getAuthorUserId() {
      return authorUserId;
    }

    public void setAuthorUserId(String authorUserId) {
      this.authorUserId = authorUserId;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }

  public ScriptDocument() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getBookingId() {
    return bookingId;
  }

  public void setBookingId(String bookingId) {
    this.bookingId = bookingId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getLastEditedByUserId() {
    return lastEditedByUserId;
  }

  public void setLastEditedByUserId(String lastEditedByUserId) {
    this.lastEditedByUserId = lastEditedByUserId;
  }

  public List<Annotation> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(List<Annotation> annotations) {
    this.annotations = annotations;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
