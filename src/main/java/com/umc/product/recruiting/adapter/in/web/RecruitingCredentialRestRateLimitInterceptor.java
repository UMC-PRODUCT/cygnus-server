package com.umc.product.recruiting.adapter.in.web;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.web.servlet.HandlerInterceptor;

import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.ratelimit.ApiRateLimitMetrics;
import com.umc.product.global.ratelimit.RateLimitBucketRegistry;
import com.umc.product.global.ratelimit.RateLimitPolicy;
import com.umc.product.global.response.ApiErrorResponseWriter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RecruitingCredentialRestRateLimitInterceptor implements HandlerInterceptor {

    private static final String APPLICATIONS_PATH = "/api/v1/recruiting/public/applications";
    private static final Set<String> POST_CREDENTIAL_PATHS = Set.of(
        APPLICATIONS_PATH + "/lookup",
        APPLICATIONS_PATH + "/submit"
    );
    private static final String POLICY_NAME = "recruiting-rest-credential";
    private static final RateLimitPolicy POLICY = new RateLimitPolicy(POLICY_NAME, 1, 5);

    private final RateLimitBucketRegistry bucketRegistry;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final ApiRateLimitMetrics metrics;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        if (!isCredentialRequest(request)) {
            return true;
        }
        String bucketKey = "rest:recruiting-credential:ip:" + request.getRemoteAddr();
        Bucket bucket = bucketRegistry.get(bucketKey, POLICY);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", String.valueOf(POLICY.requestsPerMinute()));
        if (probe.isConsumed()) {
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            metrics.record("allowed", POLICY_NAME, request.getMethod(), request.getRequestURI(), "ANONYMOUS");
            return true;
        }

        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds(probe)));
        response.setHeader("X-RateLimit-Remaining", "0");
        metrics.record("blocked", POLICY_NAME, request.getMethod(), request.getRequestURI(), "ANONYMOUS");
        errorResponseWriter.write(response, CommonErrorCode.TOO_MANY_REQUESTS);
        return false;
    }

    private boolean isCredentialRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return ("PUT".equalsIgnoreCase(request.getMethod()) && APPLICATIONS_PATH.equals(path))
            || ("POST".equalsIgnoreCase(request.getMethod()) && POST_CREDENTIAL_PATHS.contains(path));
    }

    private long retryAfterSeconds(ConsumptionProbe probe) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    }
}
