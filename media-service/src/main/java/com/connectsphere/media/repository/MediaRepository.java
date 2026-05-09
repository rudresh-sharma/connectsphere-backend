package com.connectsphere.media.repository;

import com.connectsphere.media.entity.Media;
import com.connectsphere.media.entity.MediaType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Media data.
 */
public interface MediaRepository extends JpaRepository<Media, Long> {
    List<Media> findByUploaderId(Long uploaderId);
    List<Media> findByLinkedPostIdAndDeletedFalse(Long linkedPostId);
    List<Media> findByMediaTypeAndDeletedFalse(MediaType mediaType);
}
