package com.connectsphere.post.service;

import com.connectsphere.post.dto.MediaUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Defines Media Storage business operations.
 */
public interface MediaStorageService {

    MediaUploadResponse uploadPostMedia(MultipartFile file);

    void deletePostMediaByUrl(String mediaUrl);
}
