package com.aes.exam.common.web;

import org.slf4j.MDC;

public final class RequestIdContext {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private RequestIdContext() {
    }

    public static String currentRequestId() {
        return MDC.get(REQUEST_ID_MDC_KEY);
    }
}
