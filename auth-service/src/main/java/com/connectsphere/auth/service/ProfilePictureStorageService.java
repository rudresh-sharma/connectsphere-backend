package com.connectsphere.auth.service;

import com.connectsphere.auth.dto.ProfilePictureUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines Profile Picture Storage business operations.
 */
public interface ProfilePictureStorageService {

    ProfilePictureUploadResponse upload(MultipartFile file);

    void deleteByUrl(String profilePictureUrl);
}
