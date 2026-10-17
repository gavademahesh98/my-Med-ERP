package com.edublitz.productservice.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * Adds a searchable per-request log ID to application logs.
 * The ID is generated once and reused for the request lifecycle.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int LOG_ID_DIGITS = 30;

    private String generateLogId() {
        StringBuilder digits = new StringBuilder(LOG_ID_DIGITS);
        for (int i = 0; i < LOG_ID_DIGITS; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return "LG" + digits;
    }

    private String actionFor(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "GET" -> "READ";
            case "PUT", "PATCH" -> "UPDATE";
            case "DELETE" -> "DELETE";
            default -> "OTHER";
        };
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health")
                || path.contains("/swagger-ui")
                || path.contains("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String logId = generateLogId();
        final String method = request.getMethod();
        final String action = actionFor(method);
        final String path = request.getRequestURI();
        final long startNanos = System.nanoTime();

        response.setHeader("X-Log-ID", logId);
        log.info("event=API_STARTED logId={} action={} method={} path={}",
                logId, action, method, path);

        try {
            filterChain.doFilter(request, response);
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();

            if (status >= 400) {
                log.warn("event=API_FAILED logId={} action={} method={} path={} status={} durationMs={}",
                        logId, action, method, path, status, durationMs);
            } else {
                log.info("event=API_COMPLETED logId={} action={} method={} path={} status={} durationMs={}",
                        logId, action, method, path, status, durationMs);
            }
        } catch (IOException | ServletException | RuntimeException ex) {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("event=API_FAILED logId={} action={} method={} path={} durationMs={} errorType={}",
                    logId, action, method, path, durationMs, ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
