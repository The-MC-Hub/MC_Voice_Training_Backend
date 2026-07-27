package com.mchub.events.listeners;

import com.mchub.events.PaymentSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

  private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

  @Async
  @EventListener
  public void handlePaymentSuccess(PaymentSuccessEvent event) {
    log.info(
        "Async Payment Success Event Received: User={}, Plan={}, OrderCode={}, Amount={} VND",
        event.getUserId(),
        event.getPlan(),
        event.getOrderCode(),
        event.getAmount());
    // Additional asynchronous post-payment side effects (e.g. audit log stream, notification alerts)
  }
}
