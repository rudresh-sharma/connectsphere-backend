package com.connectsphere.search.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Represents the Post Document domain model.
 */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Document(indexName = "posts")
public class PostDocument {
    @Id private String id;

    @Field(type = FieldType.Long) private Long postId;
    @Field(type = FieldType.Long) private Long authorId;
    @Field(type = FieldType.Text, analyzer = "standard") private String content;
    @Field(type = FieldType.Keyword) private String authorUsername;
    @Field(type = FieldType.Keyword) private String visibility;
}
