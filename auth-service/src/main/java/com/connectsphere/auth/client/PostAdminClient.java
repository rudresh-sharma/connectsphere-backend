package com.connectsphere.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Declares the remote contract for Post Admin integration.
 */
@Component
public class PostAdminClient {

    private final RestClient postServiceClient;

    public PostAdminClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.post-service.url:http://localhost:8081}") String postServiceUrl
    ) {
        this.postServiceClient = restClientBuilder.baseUrl(postServiceUrl).build();
    }

    public long countPosts() {
        Long response = postServiceClient.get()
                .uri("/posts/count")
                .retrieve()
                .body(Long.class);
        return response == null ? 0 : response;
    }
}
