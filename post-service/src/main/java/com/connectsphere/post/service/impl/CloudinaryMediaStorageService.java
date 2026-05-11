package com.connectsphere.post.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.post.config.CloudinaryProperties;
import com.connectsphere.post.dto.MediaUploadResponse;
import com.connectsphere.post.exception.BadRequestException;
import com.connectsphere.post.service.ImageModerationService;
import com.connectsphere.post.service.MediaStorageService;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
/**
 * Implements Cloudinary Media Storage business operations.
 */


@Service
@RequiredArgsConstructor

public class CloudinaryMediaStorageService implements MediaStorageService {

    private static final String RESOURCE_TYPE = "resource_type";
    private static final String IMAGE_RESOURCE = "image";
    private static final String VIDEO_RESOURCE = "video";

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;
    private final ImageModerationService imageModerationService;
/**
 * Uploads post media.
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public MediaUploadResponse uploadPostMedia(MultipartFile file) {
        validateFile(file);
        imageModerationService.assertSafe(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", properties.getFolder(),
                            RESOURCE_TYPE, "auto",
                            "use_filename", true,
                            "unique_filename", true
                    )
            );

            String resourceType = asString(result.get(RESOURCE_TYPE));
            String publicId = asString(result.get("public_id"));
            Double durationSeconds = asDouble(result.get("duration"));

            if (VIDEO_RESOURCE.equals(resourceType)
                    && durationSeconds != null
                    && durationSeconds > properties.getMaxVideoDurationSeconds()) {
                deleteUploadedAsset(publicId, resourceType);
                throw new BadRequestException("Video duration must be 2 minutes or less");
            }

            return new MediaUploadResponse(
                    asString(result.get("secure_url")),
                    publicId,
                    resourceType,
                    asLong(result.get("bytes")),
                    durationSeconds
            );
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new BadRequestException("Could not read uploaded media");
        } catch (Exception ex) {
            throw new BadRequestException("Could not upload media to Cloudinary");
        }
    }
/**
 * Deletes post media by url.
 * @param mediaUrl method input parameter
 */

    @Override
    public void deletePostMediaByUrl(String mediaUrl) {
        if (!StringUtils.hasText(mediaUrl)) {
            return;
        }

        String publicId = extractPublicId(mediaUrl);
        if (!StringUtils.hasText(publicId)) {
            return;
        }

        deleteUploadedAsset(publicId, inferResourceType(mediaUrl));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Media file is required");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            throw new BadRequestException("Only image and video uploads are supported");
        }
    }

    private void deleteUploadedAsset(String publicId, String resourceType) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }

        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(RESOURCE_TYPE, resourceType == null ? IMAGE_RESOURCE : resourceType)
            );
        } catch (IOException ignored) {
            // Upload validation already failed; cleanup failure should not hide the user-facing reason.
        }
    }

    private String inferResourceType(String mediaUrl) {
        String lowerUrl = mediaUrl.toLowerCase();
        if (lowerUrl.matches(".*\\.(mp4|mov|avi|mkv|webm|ogg)(\\?.*)?$")) {
            return VIDEO_RESOURCE;
        }
        return IMAGE_RESOURCE;
    }

    private String extractPublicId(String mediaUrl) {
        try {
            List<String> segments = new ArrayList<>();
            for (String segment : URI.create(mediaUrl).getPath().split("/")) {
                if (!segment.isBlank()) {
                    segments.add(segment);
                }
            }

            int uploadIndex = segments.indexOf("upload");
            if (uploadIndex < 0 || uploadIndex + 1 >= segments.size()) {
                return null;
            }

            int publicIdStart = uploadIndex + 1;
            for (int i = uploadIndex + 1; i < segments.size(); i++) {
                if (segments.get(i).matches("v\\d+")) {
                    publicIdStart = i + 1;
                    break;
                }
            }

            if (publicIdStart >= segments.size()) {
                return null;
            }

            List<String> publicIdSegments = new ArrayList<>(segments.subList(publicIdStart, segments.size()));
            String lastSegment = publicIdSegments.get(publicIdSegments.size() - 1);
            int extensionIndex = lastSegment.lastIndexOf('.');
            if (extensionIndex > 0) {
                publicIdSegments.set(publicIdSegments.size() - 1, lastSegment.substring(0, extensionIndex));
            }

            return String.join("/", publicIdSegments);
        } catch (Exception ex) {
            return null;
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
