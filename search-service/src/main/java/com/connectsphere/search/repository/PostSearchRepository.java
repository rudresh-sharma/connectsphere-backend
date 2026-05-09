package com.connectsphere.search.repository;

import com.connectsphere.search.entity.PostDocument;
import java.util.List;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * Provides persistence access for Post Search data.
 */
public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
    List<PostDocument> findByContentContaining(String keyword);
    List<PostDocument> findByAuthorUsernameContaining(String keyword);
}
