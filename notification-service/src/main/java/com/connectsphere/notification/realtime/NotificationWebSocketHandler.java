package com.connectsphere.notification.realtime;

import com.connectsphere.notification.dto.NotificationResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Supports real-time Notification WebSocket interactions.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    private static final String USER_ID_ATTRIBUTE = "userId";

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public NotificationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
/**
 * Performs the after connection established operation.
 * @param session method input parameter
 */

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session.getUri());
        if (userId == null || userId <= 0) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }
/**
 * Performs the after connection closed operation.
 * @param session method input parameter
 * @param status method input parameter
 */

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof Long id) {
            Set<WebSocketSession> sessions = sessionsByUser.get(id);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    sessionsByUser.remove(id);
                }
            }
        }
    }

/**
 * Performs the publish operation.
 * @param notification method input parameter
 */
    public void publish(NotificationResponse notification) {
        if (notification == null || notification.recipientId() == null) {
            return;
        }

        Set<WebSocketSession> sessions = sessionsByUser.get(notification.recipientId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(notification));
            for (WebSocketSession session : sessions) {
                sendQuietly(session, message);
            }
        } catch (IOException ex) {
            log.warn("Could not serialize notification {}", notification.notificationId(), ex);
        }
    }

    private void sendQuietly(WebSocketSession session, TextMessage message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(message);
            }
        } catch (IOException ex) {
            log.warn("Could not send real-time notification to session {}", session.getId(), ex);
        }
    }

    private Long resolveUserId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        for (String parameter : uri.getQuery().split("&")) {
            String[] parts = parameter.split("=", 2);
            if (parts.length == 2 && USER_ID_ATTRIBUTE.equals(parts[0])) {
                try {
                    return Long.parseLong(parts[1]);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ex) {
            log.warn("Could not close invalid notification websocket session {}", session.getId(), ex);
        }
    }
}
