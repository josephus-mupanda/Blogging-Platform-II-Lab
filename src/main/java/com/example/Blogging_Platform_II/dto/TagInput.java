package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public class TagInput {
    @NotBlank
    @Size(max = 50)
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim().toLowerCase(Locale.ROOT);
    }
}
