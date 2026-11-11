package com.example.Blogging_Platform_II.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UserUpdate {
    @NotBlank
    @Email
    @Pattern(regexp = ".+@.+\\..+", message = "must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String displayName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? null : displayName.trim();
    }
}
