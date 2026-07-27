package com.mchub.models;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "quick_replies")
public class QuickReply {

  @Id private String id;
  private String mcUserId;
  private String title;
  private String messageTemplate;
  private int displayOrder;
  private LocalDateTime createdAt;

  public QuickReply() {
    this.createdAt = LocalDateTime.now();
  }

  public QuickReply(String mcUserId, String title, String messageTemplate, int displayOrder) {
    this.mcUserId = mcUserId;
    this.title = title;
    this.messageTemplate = messageTemplate;
    this.displayOrder = displayOrder;
    this.createdAt = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMcUserId() {
    return mcUserId;
  }

  public void setMcUserId(String mcUserId) {
    this.mcUserId = mcUserId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMessageTemplate() {
    return messageTemplate;
  }

  public void setMessageTemplate(String messageTemplate) {
    this.messageTemplate = messageTemplate;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
