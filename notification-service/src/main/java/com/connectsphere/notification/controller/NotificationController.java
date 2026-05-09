package com.connectsphere.notification.controller;

import com.connectsphere.notification.dto.BulkNotificationRequest;
import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.dto.NotificationResponse;
import com.connectsphere.notification.dto.PaymentReceiptEmailRequest;
import com.connectsphere.notification.dto.WelcomeEmailRequest;
import com.connectsphere.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
/**
 * Exposes Notification API endpoints.
 */


@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")

public class NotificationController {

    private final NotificationService notificationService;
/**
 * Handles the create request.
 * @param request request payload
 * @return operation result
 */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return notificationService.createNotification(request);
    }
/**
 * Handles the bulk request.
 * @param request request payload
 */

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendBulk(@RequestBody BulkNotificationRequest request) {
        notificationService.sendBulkNotification(request);
    }
/**
 * Handles the welcome email request.
 * @param request request payload
 */

    @PostMapping("/email/welcome")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendWelcomeEmail(@Valid @RequestBody WelcomeEmailRequest request) {
        notificationService.sendWelcomeEmail(request);
    }
/**
 * Handles the payment receipt email request.
 * @param request request payload
 */

    @PostMapping("/email/payment-receipt")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sendPaymentReceiptEmail(@Valid @RequestBody PaymentReceiptEmailRequest request) {
        notificationService.sendPaymentReceiptEmail(request);
    }
/**
 * Handles the mark as read request.
 * @param id entity identifier
 */

    @PatchMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable("id") Long id) {
        notificationService.markAsRead(id);
    }
/**
 * Handles the mark all read request.
 * @param recipientId entity identifier
 */

    @PatchMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@RequestParam("recipientId") Long recipientId) {
        notificationService.markAllRead(recipientId);
    }
/**
 * Returns by recipient.
 * @param recipientId entity identifier
 * @return matching results
 */

    @GetMapping("/user/{recipientId}")
    public List<NotificationResponse> getByRecipient(@PathVariable("recipientId") Long recipientId) {
        return notificationService.getByRecipient(recipientId);
    }
/**
 * Returns unread count.
 * @param recipientId entity identifier
 * @return operation result
 */

    @GetMapping("/user/{recipientId}/unread-count")
    public long getUnreadCount(@PathVariable("recipientId") Long recipientId) {
        return notificationService.getUnreadCount(recipientId);
    }
/**
 * Handles the delete request.
 * @param id entity identifier
 */

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        notificationService.deleteNotification(id);
    }
/**
 * Deletes by user.
 * @param userId entity identifier
 */

    @DeleteMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteByUser(@PathVariable("userId") Long userId) {
        notificationService.deleteNotificationsByUser(userId);
    }
}
