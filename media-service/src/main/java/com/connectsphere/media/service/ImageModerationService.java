package com.connectsphere.media.service;

import com.connectsphere.media.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
/**
 * Provides Image Moderation business operations.
 */


@Service

public class ImageModerationService {

    private static final List<String> BLOCKED_LIKELIHOODS = List.of("LIKELY", "VERY_LIKELY");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String googleVisionApiKey;

    public ImageModerationService(
            ObjectMapper objectMapper,
            @Value("${moderation.google-vision-api-key:}") String googleVisionApiKey
    ) {
        this.objectMapper = objectMapper;
        this.googleVisionApiKey = googleVisionApiKey;
    }

/**
 * Validates safe.
 * @param file uploaded file
 */
    public void assertSafe(MultipartFile file) {
        if (file == null || file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            return;
        }
        if (googleVisionApiKey == null || googleVisionApiKey.isBlank()) {
            return;
        }

        try {
            String imageContent = Base64.getEncoder().encodeToString(file.getBytes());
            Map<String, Object> requestBody = Map.of(
                    "requests", List.of(Map.of(
                            "image", Map.of("content", imageContent),
                            "features", List.of(Map.of("type", "SAFE_SEARCH_DETECTION"))
                    ))
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://vision.googleapis.com/v1/images:annotate?key=" + googleVisionApiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            JsonNode safeSearch = objectMapper.readTree(response.body())
                    .path("responses")
                    .path(0)
                    .path("safeSearchAnnotation");
            if (isBlocked(safeSearch.path("adult").asText())
                    || isBlocked(safeSearch.path("violence").asText())
                    || isBlocked(safeSearch.path("racy").asText())) {
                throw new BadRequestException("Image failed automated safety moderation");
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ignored) {
            // Moderation provider failures should not block local development uploads.
        }
    }

    private boolean isBlocked(String likelihood) {
        return BLOCKED_LIKELIHOODS.contains(likelihood);
    }
}
