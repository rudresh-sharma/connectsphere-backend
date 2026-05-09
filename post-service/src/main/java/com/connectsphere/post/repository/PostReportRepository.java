package com.connectsphere.post.repository;

import com.connectsphere.post.entity.PostReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Post Report data.
 */
public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    List<PostReport> findByResolvedOrderByCreatedAtDesc(boolean resolved);
}
