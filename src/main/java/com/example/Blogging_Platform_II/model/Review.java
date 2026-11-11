package com.example.Blogging_Platform_II.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("reviews")
@CompoundIndex(name = "postId_1_reviewerId_1", def = "{'postId': 1, 'reviewerId': 1}", unique = true)
public class Review {
    @Id
    private String id;
    private long postId;
    private long reviewerId;
    private String reviewerName;
    private int rating;
    private String text;
    private Instant createdAt;

    public Review() {
    }

    public Review(String id, long postId, long reviewerId, String reviewerName, int rating, String text,
                  Instant createdAt) {
        this.id = id;
        this.postId = postId;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getPostId() {
        return postId;
    }

    public void setPostId(long postId) {
        this.postId = postId;
    }

    public long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
