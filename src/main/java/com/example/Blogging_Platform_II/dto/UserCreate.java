package com.example.Blogging_Platform_II.dto;

import com.example.Blogging_Platform_II.validation.UniqueAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@UniqueAccount
public record UserCreate(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Pattern(regexp = "[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+") @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String displayName) {
    public UserCreate {
        username = username == null ? null : username.strip();
        email = email == null ? null : email.strip();
        displayName = displayName == null ? null : displayName.strip();
    }
}
