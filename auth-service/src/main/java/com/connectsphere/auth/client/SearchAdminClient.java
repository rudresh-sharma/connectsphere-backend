package com.connectsphere.auth.client;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Declares the remote contract for Search Admin integration.
 */
@Component
public class SearchAdminClient {

    private final RestClient searchServiceClient;

    public SearchAdminClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.search-service.url:http://localhost:8087}") String searchServiceUrl
    ) {
        this.searchServiceClient = restClientBuilder.baseUrl(searchServiceUrl).build();
    }

    public List<HashtagSummary> getTrendingHashtags(int limit) {
        List<HashtagSummary> response = searchServiceClient.get()
                .uri(uriBuilder -> uriBuilder.path("/hashtags/trending").queryParam("limit", limit).build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return response == null ? List.of() : response;
    }

    public record HashtagSummary(Long hashtagId, String tag, long postCount) {
    }
}
