package com.talentcircle.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, String error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    // 'message' es el campo que lee el interceptor de Axios en el frontend
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, message);
    }

    public static <T> ApiResponse<T> validationError(T errors) {
        return new ApiResponse<>(false, errors, "Validation failed", "Validation failed");
    }
}
