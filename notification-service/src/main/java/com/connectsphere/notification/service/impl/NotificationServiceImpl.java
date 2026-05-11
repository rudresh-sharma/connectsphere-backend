package com.connectsphere.notification.service.impl;

import com.connectsphere.notification.client.AuthServiceClient;
import com.connectsphere.notification.client.UserSummary;
import com.connectsphere.notification.config.CacheConfig;
import com.connectsphere.notification.dto.BulkNotificationRequest;
import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.dto.NotificationResponse;
import com.connectsphere.notification.dto.PaymentReceiptEmailRequest;
import com.connectsphere.notification.dto.WelcomeEmailRequest;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.entity.NotificationType;
import com.connectsphere.notification.exception.BadRequestException;
import com.connectsphere.notification.exception.NotificationDeliveryException;
import com.connectsphere.notification.exception.ResourceNotFoundException;
import jakarta.mail.MessagingException;
import com.connectsphere.notification.repository.NotificationRepository;
import com.connectsphere.notification.realtime.NotificationWebSocketHandler;
import com.connectsphere.notification.service.NotificationService;
import feign.FeignException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/**
 * Implements Notification business operations.
 */


@Service
@RequiredArgsConstructor
@Transactional

public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final AuthServiceClient authServiceClient;
    private final JavaMailSender mailSender;
    private final CacheManager cacheManager;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    @Value("${app.mail.from:noreply@connectsphere.com}")
    private String mailFrom;
    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;
/**
 * Creates notification.
 * @param request request payload
 * @return operation result
 */

    @Override
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientId(request.recipientId())
                .actorId(request.actorId())
                .type(request.type())
                .message(request.message() != null ? request.message() : buildDefaultMessage(request))
                .targetId(request.targetId())
                .targetType(request.targetType())
                .build();

        Notification saved = notificationRepository.save(notification);
        evictRecipientCaches(saved.getRecipientId());

        NotificationResponse response = toResponse(saved);
        notificationWebSocketHandler.publish(response);

        // Send email for high-priority events
        if (request.type() == NotificationType.FOLLOW
                || request.type() == NotificationType.MENTION
                || request.type() == NotificationType.SYSTEM) {
            try {
                sendEmailAlert(request.recipientId(), buildEmailSubject(request.type()),
                        saved.getMessage());
            } catch (Exception ex) {
                log.warn("Could not send email alert for notification {}", saved.getNotificationId(), ex);
            }
        }

        return response;
    }
/**
 * Sends bulk notification.
 * @param request request payload
 */

    @Override
    public void sendBulkNotification(BulkNotificationRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new BadRequestException("message is required");
        }

        List<Long> recipientIds = request.recipientIds();
        if (recipientIds == null || recipientIds.isEmpty()) {
            recipientIds = authServiceClient.getAllUsers().stream()
                    .filter(UserSummary::active)
                    .map(UserSummary::userId)
                    .toList();
        }

        if (recipientIds.isEmpty()) {
            throw new BadRequestException("No notification recipients found");
        }

        for (Long recipientId : recipientIds) {
            Notification notification = Notification.builder()
                    .recipientId(recipientId)
                    .actorId(0L)
                    .type(NotificationType.MENTION)
                    .message(request.message().trim())
                    .build();
            Notification saved = notificationRepository.save(notification);
            evictRecipientCaches(recipientId);
            notificationWebSocketHandler.publish(toResponse(saved));
        }
    }
/**
 * Performs the mark as read operation.
 * @param notificationId entity identifier
 */

    @Override
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
        evictRecipientCaches(notification.getRecipientId());
    }
/**
 * Performs the mark all read operation.
 * @param recipientId entity identifier
 */

    @Override
    public void markAllRead(Long recipientId) {
        notificationRepository.markAllReadByRecipientId(recipientId);
        evictRecipientCaches(recipientId);
    }
/**
 * Returns by recipient.
 * @param recipientId entity identifier
 * @return matching results
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.RECIPIENT_NOTIFICATIONS_CACHE, key = "#p0")
    public List<NotificationResponse> getByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)
                .stream().map(this::toResponse).toList();
    }
/**
 * Returns unread count.
 * @param recipientId entity identifier
 * @return operation result
 */

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.UNREAD_COUNTS_CACHE, key = "#p0")
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndRead(recipientId, false);
    }
/**
 * Deletes notification.
 * @param notificationId entity identifier
 */

    @Override
    public void deleteNotification(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
        evictRecipientCaches(notification.getRecipientId());
    }
/**
 * Sends email alert.
 * @param recipientId entity identifier
 * @param subject method input parameter
 * @param body method input parameter
 */

    @Override
    public void sendEmailAlert(Long recipientId, String subject, String body) {
        try {
            UserSummary user = authServiceClient.getUserById(recipientId);
            if (user != null && user.email() != null && !user.email().isBlank()) {
                if (subject.contains("follow") || subject.contains("Follow")) {
                    sendHtmlFollowEmail(user.email(), subject, body, recipientId);
                } else {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setTo(user.email());
                    message.setSubject(subject);
                    message.setText(body);
                    message.setFrom(mailFrom);
                    mailSender.send(message);
                }
                log.info("Email sent to {} for recipientId={}", user.email(), recipientId);
            }
        } catch (FeignException ex) {
            log.warn("Could not fetch user email for recipientId={}: {}", recipientId, ex.getMessage());
        } catch (MailException ex) {
            log.warn("Could not send email to recipientId={}: {}", recipientId, ex.getMessage());
        }
    }

    private void sendHtmlFollowEmail(String toEmail, String subject, String message, Long recipientId) {
        try {
            var htmlMessage = buildFollowEmailHtml(message);
            var mimeMessage = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlMessage, true);
            helper.setFrom(mailFrom);
            mailSender.send(mimeMessage);
            log.info("HTML follow email sent successfully to recipientId={}", recipientId);
        } catch (MessagingException | MailException ex) {
            log.warn("Could not send HTML follow email to recipientId={}: {}", recipientId, ex.getMessage());
        }
    }

    private String buildFollowEmailHtml(String message) {
        String profileUrl = frontendUrl + "/profile/" + message.replace(" started following you", "").trim();
        String dashboardUrl = frontendUrl + "/dashboard";

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>ConnectSphere - New Follower</title>
            </head>
            <body style="margin:0;padding:0;background:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#1a1a2e;">
              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f5f7fa;padding:40px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                      <!-- Header -->
                      <tr>
                        <td style="padding:28px 32px;background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%);text-align:center;">
                          <h1 style="margin:0;font-size:24px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;">ConnectSphere</h1>
                          <p style="margin:8px 0 0;font-size:14px;color:rgba(255,255,255,0.85);">Social Media Platform</p>
                        </td>
                      </tr>
                      <!-- Content -->
                      <tr>
                        <td style="padding:36px 32px;">
                          <!-- Notification Icon -->
                          <table role="presentation" width="60" cellspacing="0" cellpadding="0" style="margin:0 auto 24px;">
                            <tr>
                              <td style="width:60px;height:60px;background:#f0f0ff;border-radius:50%%;text-align:center;vertical-align:middle;">
                                <span style="font-size:28px;">👤</span>
                              </td>
                            </tr>
                          </table>
                          
                          <h2 style="margin:0 0 12px;font-size:22px;font-weight:600;color:#1a1a2e;text-align:center;">New Follower!</h2>
                          
                          <p style="margin:0 0 24px;font-size:16px;line-height:1.6;color:#4a4a6a;text-align:center;">
                            %s
                          </p>
                          
                          <!-- View Profile Button -->
                          <table role="presentation" cellspacing="0" cellpadding="0" style="margin:0 auto 20px;">
                            <tr>
                              <td style="border-radius:10px;background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%);">
                                <a href="%s" style="display:block;padding:14px 32px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:10px;">
                                  View Profile
                                </a>
                              </td>
                            </tr>
                          </table>
                          
                          <!-- Alternative Link -->
                          <p style="margin:0;text-align:center;">
                            <a href="%s" style="color:#667eea;font-size:14px;text-decoration:underline;">View in ConnectSphere</a>
                          </p>
                        </td>
                      </tr>
                      <!-- Footer -->
                      <tr>
                        <td style="padding:20px 32px;background:#f8f9fc;border-top:1px solid #e9ecef;text-align:center;">
                          <p style="margin:0;font-size:13px;color:#8a8a9a;">
                            You're receiving this because you have notifications enabled.
                          </p>
                          <p style="margin:8px 0 0;font-size:13px;color:#8a8a9a;">
                            <a href="%s" style="color:#667eea;text-decoration:none;">Notification Settings</a> • 
                            <a href="%s" style="color:#667eea;text-decoration:none;">Privacy Policy</a>
                          </p>
                          <p style="margin:16px 0 0;font-size:12px;color:#a0a0b0;">
                            © 2026 ConnectSphere. All rights reserved.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                message,
                profileUrl,
                dashboardUrl,
                dashboardUrl,
                frontendUrl,
                frontendUrl
            );
    }
/**
 * Sends welcome email.
 * @param request request payload
 */

    @Override
    public void sendWelcomeEmail(WelcomeEmailRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.email());
        message.setFrom(mailFrom);
        message.setSubject("Welcome to ConnectSphere");
        message.setText(buildWelcomeEmailBody(request));
        mailSender.send(message);
        log.info("Welcome email sent to {} for recipientId={}", request.email(), request.recipientId());
    }
/**
 * Sends payment receipt email.
 * @param request request payload
 */

    @Override
    public void sendPaymentReceiptEmail(PaymentReceiptEmailRequest request) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(request.email());
            helper.setFrom(mailFrom);
            helper.setSubject("ConnectSphere Payment Receipt");
            helper.setText(buildPaymentReceiptEmailBody(request), true);
            mailSender.send(message);
            log.info("Payment receipt email sent to {} for recipientId={} postId={}", request.email(), request.recipientId(), request.postId());
        } catch (MessagingException | MailException ex) {
            throw new NotificationDeliveryException("Could not send payment receipt email", ex);
        }
    }
/**
 * Deletes notifications by user.
 * @param userId entity identifier
 */

    @Override
    public void deleteNotificationsByUser(Long userId) {
        if (userId == null) {
            throw new BadRequestException("userId is required");
        }

        notificationRepository.deleteByRecipientIdOrActorId(userId, userId);
        clearNotificationCaches();
    }

    private String buildDefaultMessage(CreateNotificationRequest request) {
        return switch (request.type()) {
            case LIKE -> "Someone liked your content";
            case COMMENT -> "Someone commented on your post";
            case REPLY -> "Someone replied to your comment";
            case FOLLOW -> "Someone started following you";
            case MENTION -> "You were mentioned in a post";
            case SYSTEM -> "You have a new account update";
        };
    }

    private String buildEmailSubject(NotificationType type) {
        return switch (type) {
            case FOLLOW -> "ConnectSphere: New follow";
            case MENTION -> "ConnectSphere: New mention";
            case SYSTEM -> "ConnectSphere: Promotion approved";
            default -> "ConnectSphere notification";
        };
    }

    private String buildWelcomeEmailBody(WelcomeEmailRequest request) {
        return """
                Hi %s,

                Welcome to ConnectSphere.

                Your account is now ready, and you can start sharing posts, uploading stories, discovering people, and growing your community.

                Account details:
                Username: @%s
                Email: %s

                You can sign in here:
                %s/login

                If you did not create this account, please reply to this email or contact support immediately.

                Thanks,
                The ConnectSphere Team
                """.formatted(
                request.fullName().trim(),
                request.username().trim(),
                request.email().trim(),
                frontendUrl
        );
    }

    private String buildPaymentReceiptEmailBody(PaymentReceiptEmailRequest request) {
        String amount = String.format("Rs %.2f", request.amountPaise() / 100.0);
        String dashboardUrl = frontendUrl + "/profile";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>ConnectSphere Payment Receipt</title>
                </head>
                <body style="margin:0;padding:0;background:#eef4fb;font-family:Arial,sans-serif;color:#172033;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#eef4fb;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 20px 60px rgba(15,23,42,0.12);">
                          <tr>
                            <td style="padding:28px 32px;background:linear-gradient(135deg,#0f3d91 0%%,#2563eb 55%%,#1cc1bd 100%%);color:#ffffff;">
                              <div style="font-size:14px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;opacity:0.92;">ConnectSphere Receipt</div>
                              <h1 style="margin:10px 0 6px;font-size:30px;line-height:1.1;">Payment received successfully</h1>
                              <p style="margin:0;font-size:15px;line-height:1.6;opacity:0.92;">Your promotion payment has been confirmed and your request is now queued for admin review.</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:30px 32px 18px;">
                              <p style="margin:0 0 14px;font-size:16px;line-height:1.7;">Hi %s,</p>
                              <p style="margin:0 0 24px;font-size:15px;line-height:1.7;color:#475467;">
                                Thanks for choosing ConnectSphere promotion tools. We have recorded your Razorpay payment and generated the receipt below for your reference.
                              </p>

                              <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="border:1px solid #d9e1ec;border-radius:18px;background:#f8fbff;">
                                <tr>
                                  <td style="padding:22px 22px 10px;">
                                    <div style="font-size:13px;font-weight:800;letter-spacing:0.06em;text-transform:uppercase;color:#175cd3;">Receipt summary</div>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:0 22px 22px;">
                                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;">Amount paid</td>
                                        <td style="padding:10px 0;color:#101828;font-size:16px;font-weight:900;" align="right">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;border-top:1px solid #e5edf7;">Promotion duration</td>
                                        <td style="padding:10px 0;color:#101828;font-size:15px;font-weight:700;border-top:1px solid #e5edf7;" align="right">%d day(s)</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;border-top:1px solid #e5edf7;">Post ID</td>
                                        <td style="padding:10px 0;color:#101828;font-size:15px;font-weight:700;border-top:1px solid #e5edf7;" align="right">#%d</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;border-top:1px solid #e5edf7;">Razorpay order ID</td>
                                        <td style="padding:10px 0;color:#101828;font-size:14px;font-weight:700;border-top:1px solid #e5edf7;" align="right">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;border-top:1px solid #e5edf7;">Razorpay payment ID</td>
                                        <td style="padding:10px 0;color:#101828;font-size:14px;font-weight:700;border-top:1px solid #e5edf7;" align="right">%s</td>
                                      </tr>
                                      <tr>
                                        <td style="padding:10px 0;color:#667085;font-size:14px;border-top:1px solid #e5edf7;">Status</td>
                                        <td style="padding:10px 0;border-top:1px solid #e5edf7;" align="right">
                                          <span style="display:inline-block;padding:6px 10px;border-radius:999px;background:#fff3cd;color:#8a5a00;font-size:12px;font-weight:900;letter-spacing:0.03em;text-transform:uppercase;">Pending approval</span>
                                        </td>
                                      </tr>
                                    </table>
                                  </td>
                                </tr>
                              </table>

                              <div style="margin:26px 0 0;padding:18px 20px;border-radius:18px;background:#f4f8ff;color:#344054;font-size:14px;line-height:1.7;">
                                Your promotion will become active after an admin approves it. If the promotion is rejected after payment capture, ConnectSphere will initiate the refund workflow and notify you by email.
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:12px 32px 30px;">
                              <a href="%s" style="display:inline-block;padding:14px 22px;border-radius:14px;background:linear-gradient(135deg,#1c76ff 0%%,#2337d2 100%%);color:#ffffff;text-decoration:none;font-size:14px;font-weight:900;">Open ConnectSphere</a>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:18px 32px 28px;border-top:1px solid #eef2f7;color:#667085;font-size:12px;line-height:1.7;">
                              This is an automated payment receipt from ConnectSphere. Keep this email for your records.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                request.fullName().trim(),
                amount,
                request.durationDays(),
                request.postId(),
                request.razorpayOrderId().trim(),
                request.razorpayPaymentId().trim(),
                dashboardUrl
        );
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getNotificationId(), n.getRecipientId(), n.getActorId(),
                n.getType(), n.getMessage(), n.getTargetId(), n.getTargetType(), n.isRead(), n.getCreatedAt());
    }

    private void evictRecipientCaches(Long recipientId) {
        evict(CacheConfig.RECIPIENT_NOTIFICATIONS_CACHE, recipientId);
        evict(CacheConfig.UNREAD_COUNTS_CACHE, recipientId);
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }

    private void clearNotificationCaches() {
        clear(CacheConfig.RECIPIENT_NOTIFICATIONS_CACHE);
        clear(CacheConfig.UNREAD_COUNTS_CACHE);
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
