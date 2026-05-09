package com.connectsphere.notification.service;

import com.connectsphere.notification.dto.BulkNotificationRequest;
import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.dto.NotificationResponse;
import com.connectsphere.notification.dto.PaymentReceiptEmailRequest;
import com.connectsphere.notification.dto.WelcomeEmailRequest;
import java.util.List;

/**
 * Defines Notification business operations.
 */
public interface NotificationService {
    NotificationResponse createNotification(CreateNotificationRequest request);
    void sendBulkNotification(BulkNotificationRequest request);
    void markAsRead(Long notificationId);
    void markAllRead(Long recipientId);
    List<NotificationResponse> getByRecipient(Long recipientId);
    long getUnreadCount(Long recipientId);
    void deleteNotification(Long notificationId);
    void sendEmailAlert(Long recipientId, String subject, String body);
    void sendWelcomeEmail(WelcomeEmailRequest request);
    void sendPaymentReceiptEmail(PaymentReceiptEmailRequest request);
    void deleteNotificationsByUser(Long userId);
}
