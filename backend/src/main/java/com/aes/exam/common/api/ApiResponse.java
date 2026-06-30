package com.aes.exam.common.api;

import com.aes.exam.common.error.ErrorCode;
import com.aes.exam.common.web.RequestIdContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    String code,
    String message,
    T data,
    String requestId,
    OffsetDateTime timestamp
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            true,
            ErrorCode.OK.code(),
            ErrorCode.OK.defaultMessage(),
            data,
            RequestIdContext.currentRequestId(),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.defaultMessage());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
            false,
            errorCode.code(),
            message,
            null,
            RequestIdContext.currentRequestId(),
            OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
