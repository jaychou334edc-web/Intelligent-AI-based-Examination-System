package com.aes.exam.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    OK("OK", "请求成功", HttpStatus.OK),
    VALIDATION_FAILED("VALIDATION_FAILED", "请求参数校验失败", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "用户未登录或登录已失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("FORBIDDEN", "没有权限执行该操作", HttpStatus.FORBIDDEN),
    NOT_FOUND("NOT_FOUND", "请求资源不存在", HttpStatus.NOT_FOUND),
    BUSINESS_ERROR("BUSINESS_ERROR", "业务处理失败", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "系统内部错误", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public String defaultMessage() {
        return defaultMessage;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
