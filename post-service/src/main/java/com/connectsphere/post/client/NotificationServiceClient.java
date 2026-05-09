package com.connectsphere.post.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Declares the remote contract for Notification Service integration.
 */
@FeignClient(name = "notification-service", url = "${clients.notification-service.url:http://localhost:8085}")
public interface NotificationServiceClient {

    @PostMapping("/notifications")
    void createNotification(@RequestBody CreateNotificationRequest request);

    @PostMapping("/notifications/email/payment-receipt")
    void sendPaymentReceiptEmail(@RequestBody PaymentReceiptEmailRequest request);
}
