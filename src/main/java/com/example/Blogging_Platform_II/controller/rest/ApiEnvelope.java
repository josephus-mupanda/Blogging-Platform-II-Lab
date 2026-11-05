package com.example.Blogging_Platform_II.controller.rest;

public record ApiEnvelope<T>(String status, String message, T data) {
    public static <T> ApiEnvelope<T> ok(String message, T data) {
        return new ApiEnvelope<>("success", message, data);
    }

    public static <T> ApiEnvelope<T> error(String message, T data) {
        return new ApiEnvelope<>("error", message, data);
    }
}
