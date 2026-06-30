package com.aes.exam.common.security;

public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityContext> CONTEXT = new ThreadLocal<>();

    private SecurityContextHolder() {
    }

    public static SecurityContext current() {
        return CONTEXT.get();
    }

    static void set(SecurityContext context) {
        CONTEXT.set(context);
    }

    static void clear() {
        CONTEXT.remove();
    }
}
