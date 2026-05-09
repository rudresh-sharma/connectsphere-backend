package com.connectsphere.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Declares the remote contract for Account Cleanup integration.
 */
@Component
public class AccountCleanupClient {

    private final RestClient postServiceClient;
    private final RestClient followServiceClient;
    private final RestClient commentServiceClient;
    private final RestClient likeServiceClient;
    private final RestClient notificationServiceClient;
    private final RestClient mediaServiceClient;

    public AccountCleanupClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.post-service.url:http://localhost:8081}") String postServiceUrl,
            @Value("${clients.follow-service.url:http://localhost:8082}") String followServiceUrl,
            @Value("${clients.comment-service.url:http://localhost:8083}") String commentServiceUrl,
            @Value("${clients.like-service.url:http://localhost:8084}") String likeServiceUrl,
            @Value("${clients.notification-service.url:http://localhost:8085}") String notificationServiceUrl,
            @Value("${clients.media-service.url:http://localhost:8086}") String mediaServiceUrl
    ) {
        this.postServiceClient = restClientBuilder.baseUrl(postServiceUrl).build();
        this.followServiceClient = restClientBuilder.baseUrl(followServiceUrl).build();
        this.commentServiceClient = restClientBuilder.baseUrl(commentServiceUrl).build();
        this.likeServiceClient = restClientBuilder.baseUrl(likeServiceUrl).build();
        this.notificationServiceClient = restClientBuilder.baseUrl(notificationServiceUrl).build();
        this.mediaServiceClient = restClientBuilder.baseUrl(mediaServiceUrl).build();
    }

    public void deleteUserData(Long userId) {
        notificationServiceClient.delete()
                .uri("/notifications/user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete notification data for user " + userId);
                })
                .toBodilessEntity();

        likeServiceClient.delete()
                .uri("/likes/user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete like data for user " + userId);
                })
                .toBodilessEntity();

        commentServiceClient.delete()
                .uri("/comments/user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete comment data for user " + userId);
                })
                .toBodilessEntity();

        followServiceClient.delete()
                .uri("/follows/user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete follow data for user " + userId);
                })
                .toBodilessEntity();

        mediaServiceClient.delete()
                .uri("/media/user/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete media data for user " + userId);
                })
                .toBodilessEntity();

        postServiceClient.delete()
                .uri("/posts/author/{userId}", userId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new IllegalStateException("Could not delete post data for user " + userId);
                })
                .toBodilessEntity();
    }
}
