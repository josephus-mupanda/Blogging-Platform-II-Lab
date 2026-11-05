package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record TagInput(@NotBlank @Size(max = 50) String name) {
    public TagInput {
        name = name == null ? null : name.strip().toLowerCase(Locale.ROOT);
    }
}
