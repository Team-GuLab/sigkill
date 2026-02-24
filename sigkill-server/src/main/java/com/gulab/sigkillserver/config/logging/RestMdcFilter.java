package com.gulab.sigkillserver.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestMdcFilter extends OncePerRequestFilter {

    private static final String REST_API_PREFIX = "/api/";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(REST_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        MDC.put("channel", "REST");
        MDC.put("traceId", traceId);
        MDC.put("httpMethod", request.getMethod());
        MDC.put("path", request.getRequestURI());

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("path");
            MDC.remove("httpMethod");
            MDC.remove("traceId");
            MDC.remove("channel");
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String requestTraceId = request.getHeader(REQUEST_ID_HEADER);
        if (StringUtils.hasText(requestTraceId)) {
            return requestTraceId;
        }
        return UUID.randomUUID().toString();
    }
}
