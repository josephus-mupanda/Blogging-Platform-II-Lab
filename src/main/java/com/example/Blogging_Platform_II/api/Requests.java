
package com.example.Blogging_Platform_II.api;

import com.example.Blogging_Platform_II.model.Blog.Role;
import com.example.Blogging_Platform_II.validation.UniqueAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class Requests {
    private Requests() {}

    @UniqueAccount
    public record Registration(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "[^:\\p{Cntrl}]+", message = "must not contain a colon or control character")
            String username,
            @NotBlank @Email @Pattern(regexp = "[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+") @Size(max = 255) String email,
            @NotBlank @Size(min = 6, max = 128) String password,
            @NotBlank @Size(max = 100) String displayName,
            @NotNull Role role) {
        public Registration {
            username = trim(username);
            email = trim(email);
            displayName = trim(displayName);
        }
    }

    public record AccountUpdate(
            @NotBlank @Email @Pattern(regexp = "[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+") @Size(max = 255) String email,
            @NotBlank @Size(max = 100) String displayName) {
        public AccountUpdate {
            email = trim(email);
            displayName = trim(displayName);
        }
    }

    public record AccountStatus(@NotNull Boolean active) {}

    public record PostInput(@NotBlank @Size(max = 200) String title, @NotBlank String body,
                            @Size(max = 100) List<@NotNull @Positive Long> tagIds) {
        public PostInput {
            title = trim(title);
            body = trim(body);
        }
    }

    public record TagInput(@NotBlank @Size(max = 50) String name) {
        public TagInput {
            name = name == null ? null : name.strip().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record CommentInput(@NotBlank String body) {
        public CommentInput {
            body = trim(body);
        }
    }

    public record ReviewInput(@NotNull @Min(1) @Max(5) Integer rating, String text) {
        public ReviewInput {
            text = text == null ? "" : text.strip();
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
