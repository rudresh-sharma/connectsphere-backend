package com.connectsphere.comment.repository;

import com.connectsphere.comment.entity.CommentReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Provides persistence access for Comment Report data.
 */
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    List<CommentReport> findByResolvedOrderByCreatedAtDesc(boolean resolved);
}
