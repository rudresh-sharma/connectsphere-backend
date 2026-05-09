package com.connectsphere.auth.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connectsphere.auth.config.CloudinaryProperties;
import com.connectsphere.auth.dto.ProfilePictureUploadResponse;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
/**
 * Implements Cloudinary Profile Picture Storage business operations.
 */


@Service
@ConditionalOnBean(Cloudinary.class)

public class CloudinaryProfilePictureStorageService implements ProfilePictureStorageService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryProfilePictureStorageService.class);

    private final Cloudinary cloudinary;
    private final CloudinaryProperties properties;

    @Autowired
    public CloudinaryProfilePictureStorageService(Cloudinary cloudinary, CloudinaryProperties properties) {
        this.cloudinary = cloudinary;
        this.properties = properties;
    }
/**
 * Performs the upload operation.
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public ProfilePictureUploadResponse upload(MultipartFile file) {
        if (cloudinary == null) {
            throw new BadRequestException("Profile picture upload is not configured");
        }

        validate(file);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", properties.getFolder(),
                            "resource_type", "image",
                            "use_filename", true,
                            "unique_filename", true,
                            "transformation", "c_fill,g_face,w_500,h_500,q_auto,f_auto"
                    )
            );

            return new ProfilePictureUploadResponse(
                    value(result.get("secure_url")),
                    value(result.get("public_id"))
            );
        } catch (IOException ex) {
            throw new BadRequestException("Could not read profile picture");
        } catch (Exception ex) {
            throw new BadRequestException("Could not upload profile picture");
        }
    }
/**
 * Deletes by url.
 * @param profilePictureUrl method input parameter
 */

    @Override
    public void deleteByUrl(String profilePictureUrl) {
        if (cloudinary == null) {
            return;
        }

        if (!StringUtils.hasText(profilePictureUrl)) {
            return;
        }

        String publicId = extractPublicId(profilePictureUrl);
        if (!StringUtils.hasText(publicId)) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (Exception ignored) {
            log.debug("Failed to delete profile picture from Cloudinary", ignored);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Profile picture is required");
        }

        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are supported for profile pictures");
        }
    }

    private String value(Object object) {
        return object == null ? null : object.toString();
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
}
