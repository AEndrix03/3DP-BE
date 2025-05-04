package it.aredegalli.printer.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtil {

    public static HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    public static String getClientIp() {
        return getClientIp(getCurrentRequest());
    }

    public static String getClientAgent() {
        return getCurrentRequest().getHeader("User-Agent");
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    public static String getClientAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
