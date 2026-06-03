package com.football501.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter for MVP scale.
 * Limits unauthenticated requests to 10/min and authenticated to 100/min per IP.
 *
 * Not for production at scale — replace with Redis-backed rate limiting
 * (e.g. Bucket4j + Redis) when horizontal scaling is needed.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int UNAUTHENTICATED_LIMIT = 10;
    private static final int AUTHENTICATED_LIMIT = 100;
    private static final long WINDOW_MS = 60_000;

    private record Window(AtomicInteger count, long resetAt) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip rate limiting for health checks
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = request.getRemoteAddr();
        boolean isJwt = "jwt".equals(request.getAttribute(OptionalJwtFilter.AUTH_TYPE_ATTR));
        String key = isJwt ? "auth:" + ip : "anon:" + ip;
        int limit = isJwt ? AUTHENTICATED_LIMIT : UNAUTHENTICATED_LIMIT;

        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, w) -> {
            if (w == null || now > w.resetAt()) {
                return new Window(new AtomicInteger(1), now + WINDOW_MS);
            }
            w.count().incrementAndGet();
            return w;
        });

        // Clean up expired windows occasionally
        if (now > window.resetAt()) {
            windows.remove(key);
        }

        if (window.count().get() > limit) {
            log.warn("Rate limit exceeded for {} (count={}, limit={})", key, window.count().get(), limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
