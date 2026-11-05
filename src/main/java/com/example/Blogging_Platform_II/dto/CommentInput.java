package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentInput(@NotBlank String body) {
    public CommentInput {
        body = body == null ? null : body.strip();
    }
}
