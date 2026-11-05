package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostInput(@NotBlank @Size(max = 200) String title, @NotBlank String body,
                        @Size(max = 100) List<@NotNull @Positive Long> tagIds) {
    public PostInput {
        title = title == null ? null : title.strip();
        body = body == null ? null : body.strip();
    }
}
