package com.edublitz.productservice.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiAction(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        HttpServletRequest request =
                attributes != null ? attributes.getRequest() : null;

        String method = request != null ? request.getMethod() : "UNKNOWN";
        String path = request != null ? request.getRequestURI() : "UNKNOWN";
        String action = joinPoint.getSignature().getName();
        long start = System.currentTimeMillis();

        log.info("event=API_STARTED method={} path={} action={}", method, path, action);

        try {
            Object result = joinPoint.proceed();
            long durationMs = System.currentTimeMillis() - start;

            int status = result instanceof ResponseEntity<?> response
                    ? response.getStatusCode().value() : 200;

            log.info(
                    "event=API_COMPLETED method={} path={} action={} status={} durationMs={}",
                    method, path, action, status, durationMs
            );
            return result;
        } catch (Throwable ex) {
            long durationMs = System.currentTimeMillis() - start;
            log.error(
                    "event=API_FAILED method={} path={} action={} durationMs={} exceptionType={}",
                    method, path, action, durationMs, ex.getClass().getSimpleName()
            );
            throw ex;
        }
    }
}
