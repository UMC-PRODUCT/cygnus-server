package com.umc.product.global.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.web.servlet.HandlerInterceptor;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.response.ApiErrorResponseWriter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ApiRateLimitInterceptor implements HandlerInterceptor {

    private static final String HEADER_RETRY_AFTER = "Retry-After";
    private static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    private static final int KEY_HASH_LENGTH = 12;

    private final RateLimitClientKeyResolver clientKeyResolver;
    private final RateLimitRouteResolver routeResolver;
    private final RateLimitPolicyResolver policyResolver;
    private final RateLimitBucketRegistry bucketRegistry;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final ApiRateLimitMetrics metrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        String routePattern = routeResolver.resolve(request);
        RateLimitClientKey clientKey = clientKeyResolver.resolve(request);
        Optional<RateLimitPolicy> policy = policyResolver.resolve(
            request.getMethod(),
            routePattern,
            request.getRequestURI(),
            clientKey.authenticated()
        );

        if (policy.isEmpty()) {
            return true;
        }

        RateLimitPolicy resolvedPolicy = policy.get();
        String bucketKey = clientKey.value() + ":" + request.getMethod() + ":" + routePattern;
        Bucket bucket = bucketRegistry.get(bucketKey, resolvedPolicy);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader(HEADER_RATE_LIMIT_LIMIT, String.valueOf(resolvedPolicy.requestsPerSecond()));
        if (probe.isConsumed()) {
            response.setHeader(HEADER_RATE_LIMIT_REMAINING, String.valueOf(probe.getRemainingTokens()));
            metrics.record("allowed", resolvedPolicy.name(), request.getMethod(), routePattern, clientKey.clientType());
            return true;
        }

        long retryAfterSeconds = retryAfterSeconds(probe);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setHeader(HEADER_RATE_LIMIT_REMAINING, "0");
        metrics.record("blocked", resolvedPolicy.name(), request.getMethod(), routePattern, clientKey.clientType());
        log.warn(
            "api_rate_limit_blocked rule={} method={} routePattern={} clientType={} keyHash={} retryAfterSeconds={}",
            resolvedPolicy.name(),
            request.getMethod(),
            routePattern,
            clientKey.clientType(),
            hashKey(bucketKey),
            retryAfterSeconds
        );
        errorResponseWriter.write(response, CommonErrorCode.TOO_MANY_REQUESTS);
        return false;
    }

    private long retryAfterSeconds(ConsumptionProbe probe) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    }

    private String hashKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, KEY_HASH_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            return "sha256-unavailable";
        }
    }
}
