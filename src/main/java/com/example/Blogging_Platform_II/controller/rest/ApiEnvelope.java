package com.example.Blogging_Platform_II.controller.rest;

/** Every REST response is wrapped in this shape: status, message and data. */
public class ApiEnvelope<T> {
    private String status;
    private String message;
    private T data;

    public ApiEnvelope(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiEnvelope<T> ok(String message, T data) {
        return new ApiEnvelope<>("success", message, data);
    }

    public static <T> ApiEnvelope<T> error(String message, T data) {
        return new ApiEnvelope<>("error", message, data);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
