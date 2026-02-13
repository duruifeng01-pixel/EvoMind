package com.evomind.api.model;

public record ApiResponse<T>(int code, String message, String requestId, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", java.util.UUID.randomUUID().toString(), data);
    }
}
