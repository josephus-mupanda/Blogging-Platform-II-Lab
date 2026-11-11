package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.NotBlank;

public class CommentInput {
    @NotBlank
    private String body;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body == null ? null : body.trim();
    }
}
