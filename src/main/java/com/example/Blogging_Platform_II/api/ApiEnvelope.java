
package com.example.Blogging_Platform_II.api;

public record ApiEnvelope<T>(String status, String message, T data) {
    public static <T> ApiEnvelope<T> ok(String message, T data) {
        return new ApiEnvelope<>("success", message, data);
    }

    public static <T> ApiEnvelope<T> error(String message, T data) {
        return new ApiEnvelope<>("error", message, data);
    }
}
