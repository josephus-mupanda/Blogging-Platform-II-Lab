package com.example.Blogging_Platform_II.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("comments")
@CompoundIndex(name = "postId_1_createdAt_1", def = "{'postId': 1, 'createdAt': 1}")
public record Comment(@Id String id, long postId, long authorId, String authorName,
                      String body, Instant createdAt, Instant deletedAt) {}
