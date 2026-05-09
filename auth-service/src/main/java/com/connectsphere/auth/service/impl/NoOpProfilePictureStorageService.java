package com.connectsphere.auth.service.impl;

import com.connectsphere.auth.dto.ProfilePictureUploadResponse;
import com.connectsphere.auth.exception.BadRequestException;
import com.connectsphere.auth.service.ProfilePictureStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
/**
 * Implements No Op Profile Picture Storage business operations.
 */


@Service
@ConditionalOnMissingBean(CloudinaryProfilePictureStorageService.class)

public class NoOpProfilePictureStorageService implements ProfilePictureStorageService {
/**
 * Performs the upload operation.
 * @param file uploaded file
 * @return operation result
 */

    @Override
    public ProfilePictureUploadResponse upload(MultipartFile file) {
        throw new BadRequestException("Profile picture upload is not configured. Please configure Cloudinary.");
    }
/**
 * Deletes by url.
 * @param profilePictureUrl method input parameter
 */

    @Override
    public void deleteByUrl(String profilePictureUrl) {
        // No-op - nothing to delete
    }
}