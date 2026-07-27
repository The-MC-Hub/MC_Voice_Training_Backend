package com.mchub.events;

import com.mchub.enums.SubscriptionPlan;
import java.time.LocalDateTime;

public class PaymentSuccessEvent {

  private final String userId;
  private final SubscriptionPlan plan;
  private final long orderCode;
  private final int amount;
  private final LocalDateTime timestamp;

  public PaymentSuccessEvent(String userId, SubscriptionPlan plan, long orderCode, int amount) {
    this.userId = userId;
    this.plan = plan;
    this.orderCode = orderCode;
    this.amount = amount;
    this.timestamp = LocalDateTime.now();
  }

  public String getUserId() {
    return userId;
  }

  public SubscriptionPlan getPlan() {
    return plan;
  }

  public long getOrderCode() {
    return orderCode;
  }

  public int getAmount() {
    return amount;
  }

  public LocalDateTime getTimestamp() {
    return timestamp;
  }
}
