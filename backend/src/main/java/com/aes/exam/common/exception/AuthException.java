package com.aes.exam.common.exception;

import com.aes.exam.common.error.ErrorCode;

public class AuthException extends RuntimeException {

    private final ErrorCode errorCode;

    public AuthException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public AuthException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
