package com.connectsphere.notification.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.connectsphere.notification.client.AuthServiceClient;
import com.connectsphere.notification.client.UserSummary;
import com.connectsphere.notification.dto.BulkNotificationRequest;
import com.connectsphere.notification.dto.CreateNotificationRequest;
import com.connectsphere.notification.dto.NotificationResponse;
import com.connectsphere.notification.dto.PaymentReceiptEmailRequest;
import com.connectsphere.notification.dto.WelcomeEmailRequest;
import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.entity.NotificationType;
import com.connectsphere.notification.exception.BadRequestException;
import com.connectsphere.notification.exception.ResourceNotFoundException;
import com.connectsphere.notification.realtime.NotificationWebSocketHandler;
import com.connectsphere.notification.repository.NotificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private NotificationWebSocketHandler notificationWebSocketHandler;

    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, authServiceClient, mailSender, cacheManager, notificationWebSocketHandler);
        ReflectionTestUtils.setField(service, "mailFrom", "noreply@connectsphere.com");
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void createNotificationUsesDefaultMessageAndPublishesWebSocketEvent() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(9L);
            notification.setCreatedAt(Instant.now());
            return notification;
        });

        NotificationResponse response = service.createNotification(new CreateNotificationRequest(
                2L,
                1L,
                NotificationType.LIKE,
                null,
                13L,
                "POST"
        ));

        assertEquals("Someone liked your content", response.message());
        verify(notificationWebSocketHandler).publish(response);
    }

    @Test
    void createNotificationSendsEmailForFollowEvents() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(10L);
            notification.setCreatedAt(Instant.now());
            return notification;
        });
        when(authServiceClient.getUserById(2L)).thenReturn(new UserSummary(2L, "anuj", "Anuj", "anuj@example.com", null, true));

        service.createNotification(new CreateNotificationRequest(
                2L,
                1L,
                NotificationType.FOLLOW,
                null,
                13L,
                "USER"
        ));

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void createNotificationUsesProvidedMessage() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(11L);
            notification.setCreatedAt(Instant.now());
            return notification;
        });

        NotificationResponse response = service.createNotification(new CreateNotificationRequest(
                2L,
                1L,
                NotificationType.SYSTEM,
                "Custom message",
                null,
                null
        ));

        assertEquals("Custom message", response.message());
    }

    @Test
    void sendBulkNotificationRejectsBlankMessage() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.sendBulkNotification(new BulkNotificationRequest(List.of(1L), " ")));

        assertEquals("message is required", exception.getMessage());
    }

    @Test
    void sendBulkNotificationLoadsActiveUsersWhenRecipientsMissing() {
        when(authServiceClient.getAllUsers()).thenReturn(List.of(
                new UserSummary(1L, "a", "A", "a@example.com", null, true),
                new UserSummary(2L, "b", "B", "b@example.com", null, false),
                new UserSummary(3L, "c", "C", "c@example.com", null, true)
        ));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setNotificationId(20L);
            notification.setCreatedAt(Instant.now());
            return notification;
        });

        service.sendBulkNotification(new BulkNotificationRequest(null, "hello all"));

        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
        verify(notificationWebSocketHandler, org.mockito.Mockito.times(2)).publish(any(NotificationResponse.class));
    }

    @Test
    void sendBulkNotificationRejectsWhenNoRecipientsFound() {
        when(authServiceClient.getAllUsers()).thenReturn(List.of(new UserSummary(2L, "b", "B", "b@example.com", null, false)));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.sendBulkNotification(new BulkNotificationRequest(null, "hello all")));

        assertEquals("No notification recipients found", exception.getMessage());
    }

    @Test
    void markAsReadUpdatesNotification() {
        Notification notification = notification(9L, 2L, 1L, NotificationType.LIKE, "msg");
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(notification));

        service.markAsRead(9L);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsReadRejectsMissingNotification() {
        when(notificationRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markAsRead(9L));
    }

    @Test
    void markAllReadDelegatesToRepository() {
        service.markAllRead(2L);

        verify(notificationRepository).markAllReadByRecipientId(2L);
    }

    @Test
    void getByRecipientMapsNotifications() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(
                notification(9L, 2L, 1L, NotificationType.LIKE, "msg")
        ));

        List<NotificationResponse> responses = service.getByRecipient(2L);

        assertEquals(1, responses.size());
        assertEquals("msg", responses.get(0).message());
    }

    @Test
    void getUnreadCountDelegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndRead(2L, false)).thenReturn(3L);

        assertEquals(3L, service.getUnreadCount(2L));
    }

    @Test
    void deleteNotificationDeletesExistingNotification() {
        Notification notification = notification(9L, 2L, 1L, NotificationType.LIKE, "msg");
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(notification));

        service.deleteNotification(9L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void sendEmailAlertSkipsBlankEmail() {
        when(authServiceClient.getUserById(2L)).thenReturn(new UserSummary(2L, "anuj", "Anuj", " ", null, true));

        service.sendEmailAlert(2L, "subject", "body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmailAlertBuildsSimpleMailMessage() {
        when(authServiceClient.getUserById(2L)).thenReturn(new UserSummary(2L, "anuj", "Anuj", "anuj@example.com", null, true));

        service.sendEmailAlert(2L, "subject", "body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("subject", captor.getValue().getSubject());
        assertEquals("body", captor.getValue().getText());
    }

    @Test
    void sendWelcomeEmailBuildsWelcomeMessage() {
        service.sendWelcomeEmail(new WelcomeEmailRequest(2L, "anuj@example.com", "anuj", "Anuj"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("Welcome to ConnectSphere"));
        assertTrue(captor.getValue().getText().contains("@anuj"));
    }

    @Test
    void sendWelcomeEmailPropagatesMailFailure() {
        doThrow(new RuntimeException("mail down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class,
                () -> service.sendWelcomeEmail(new WelcomeEmailRequest(2L, "anuj@example.com", "anuj", "Anuj")));
    }

    @Test
    void sendPaymentReceiptEmailBuildsHtmlEmail() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendPaymentReceiptEmail(new PaymentReceiptEmailRequest(
                2L,
                "anuj@example.com",
                "anuj",
                "Anuj",
                13L,
                "order_1",
                "pay_1",
                2500,
                5
        ));

        verify(mailSender).send(mimeMessage);
        assertTrue(mimeMessage.getContent().toString().contains("Payment received successfully"));
        assertTrue(mimeMessage.getContent().toString().contains("order_1"));
    }

    @Test
    void sendPaymentReceiptEmailWrapsFailure() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("mail down"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.sendPaymentReceiptEmail(new PaymentReceiptEmailRequest(
                        2L,
                        "anuj@example.com",
                        "anuj",
                        "Anuj",
                        13L,
                        "order_1",
                        "pay_1",
                        2500,
                        5
                )));

        assertEquals("Could not send payment receipt email", exception.getMessage());
    }

    @Test
    void deleteNotificationsByUserRequiresUserId() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.deleteNotificationsByUser(null));

        assertEquals("userId is required", exception.getMessage());
    }

    @Test
    void deleteNotificationsByUserDelegatesToRepository() {
        service.deleteNotificationsByUser(2L);

        verify(notificationRepository).deleteByRecipientIdOrActorId(2L, 2L);
    }

    private static Notification notification(Long id, Long recipientId, Long actorId, NotificationType type, String message) {
        Notification notification = Notification.builder()
                .recipientId(recipientId)
                .actorId(actorId)
                .type(type)
                .message(message)
                .build();
        notification.setNotificationId(id);
        notification.setCreatedAt(Instant.now());
        return notification;
    }
}
