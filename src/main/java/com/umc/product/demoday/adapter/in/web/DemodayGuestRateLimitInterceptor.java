package com.umc.product.demoday.adapter.in.web;

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

/**
 * 게스트 입장 코드 제출 엔드포인트를 IP 단위로 레이트리밋한다. 이 엔드포인트는 인증 이전 단계라
 * 회원 식별자가 없으므로, 발신 IP를 키로 쓰는 recruiting 도메인 익명 제출 엔드포인트와 같은 방식을 쓴다.
 */
@RequiredArgsConstructor
public class DemodayGuestRateLimitInterceptor implements HandlerInterceptor {

    private static final String POLICY_NAME = "demoday-guest-participation";

    private final RateLimitBucketRegistry bucketRegistry;
    private final ApiErrorResponseWriter errorResponseWriter;
    private final ApiRateLimitMetrics metrics;
    private final RateLimitPolicy policy;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
        throws Exception {
        String bucketKey = "rest:demoday-guest-participation:ip:" + request.getRemoteAddr();
        Bucket bucket = bucketRegistry.get(bucketKey, policy);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.requestsPerMinute()));

        if (!probe.isConsumed()) {
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds(probe)));
            response.setHeader("X-RateLimit-Remaining", "0");
            metrics.record("blocked", POLICY_NAME, request.getMethod(), request.getRequestURI(), "ANONYMOUS");
            errorResponseWriter.write(response, CommonErrorCode.TOO_MANY_REQUESTS);
            return false;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
        metrics.record("allowed", POLICY_NAME, request.getMethod(), request.getRequestURI(), "ANONYMOUS");
        return true;
    }

    private long retryAfterSeconds(ConsumptionProbe probe) {
        return Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
    }
}
