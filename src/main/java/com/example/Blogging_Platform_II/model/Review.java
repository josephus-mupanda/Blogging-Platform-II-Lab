package com.example.Blogging_Platform_II.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reviews")
@CompoundIndex(name = "postId_1_reviewerId_1", def = "{'postId': 1, 'reviewerId': 1}", unique = true)
public record Review(@Id String id, long postId, long reviewerId, String reviewerName,
                     int rating, String text, Instant createdAt) {}
