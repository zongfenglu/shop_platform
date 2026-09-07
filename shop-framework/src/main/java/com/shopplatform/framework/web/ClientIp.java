package com.shopplatform.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIp {

    private ClientIp() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwarded) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        if (!StringUtils.hasText(ip)) {
            return null;
        }
        return ip.length() > 64 ? ip.substring(0, 64) : ip;
    }
}
