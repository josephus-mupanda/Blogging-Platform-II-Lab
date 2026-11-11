package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ReviewInput {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String text = "";

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text.trim();
    }
}
