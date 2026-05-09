package com.connectsphere.notification.repository;

import com.connectsphere.notification.entity.Notification;
import com.connectsphere.notification.entity.NotificationType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Provides persistence access for Notification data.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    List<Notification> findByRecipientIdAndReadOrderByCreatedAtDesc(Long recipientId, boolean read);

    long countByRecipientIdAndRead(Long recipientId, boolean read);

    List<Notification> findByTypeOrderByCreatedAtDesc(NotificationType type);

    List<Notification> findByActorIdAndTargetId(Long actorId, Long targetId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientId = :recipientId AND n.read = false")
    int markAllReadByRecipientId(@Param("recipientId") Long recipientId);

    @Modifying
    void deleteByRecipientIdOrActorId(Long recipientId, Long actorId);
}
