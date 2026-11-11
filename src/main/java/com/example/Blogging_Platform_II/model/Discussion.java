package com.example.Blogging_Platform_II.model;

import java.util.List;

public class Discussion {
    private List<Review> reviews = List.of();
    private double averageRating;

    public Discussion() {
    }

    public Discussion(List<Review> reviews, double averageRating) {
        this.reviews = reviews;
        this.averageRating = averageRating;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
}
