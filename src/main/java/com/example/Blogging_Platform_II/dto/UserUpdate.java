package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserUpdate(
        @NotBlank @Email @Pattern(regexp = "[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+") @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String displayName) {
    public UserUpdate {
        email = email == null ? null : email.strip();
        displayName = displayName == null ? null : displayName.strip();
    }
}
