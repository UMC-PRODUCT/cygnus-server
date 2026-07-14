package com.umc.product.authorization.adapter.in.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectPolicyFacts;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

class AuthorizationRequestSubjectContextTest {

    private static final Instant EVALUATED_AT = Instant.parse("2026-07-13T00:00:00Z");

    @Test
    @DisplayName("같은 요청에서는 subject를 한 번만 로드하고 동일 객체를 반환한다")
    void loadsSubjectOncePerRequest() {
        AuthorizationRequestSubjectContext context = new AuthorizationRequestSubjectContext();
        SubjectAttributes subject = subject(1L, EVALUATED_AT);
        AtomicInteger loadCount = new AtomicInteger();

        SubjectAttributes first = context.getOrLoad(1L, ignored -> {
            loadCount.incrementAndGet();
            return subject;
        });
        SubjectAttributes second = context.getOrLoad(1L, ignored -> {
            loadCount.incrementAndGet();
            return subject(1L, EVALUATED_AT.plusSeconds(1));
        });

        assertThat(first).isSameAs(subject);
        assertThat(second).isSameAs(subject);
        assertThat(context.require(1L)).isSameAs(subject);
        assertThat(loadCount).hasValue(1);
    }

    @Test
    @DisplayName("seed된 subject와 다른 member 요청은 정책 평가 실패로 차단한다")
    void rejectsDifferentMemberInSameRequest() {
        AuthorizationRequestSubjectContext context = new AuthorizationRequestSubjectContext();
        context.getOrLoad(1L, ignored -> subject(1L, EVALUATED_AT));

        assertThatThrownBy(() -> context.require(2L))
            .isInstanceOf(AuthorizationDomainException.class)
            .hasFieldOrPropertyWithValue("baseCode", AuthorizationErrorCode.POLICY_EVALUATION_FAILED);
    }

    @Test
    @DisplayName("동시 요청의 request context는 subject와 evaluatedAt을 서로 공유하지 않는다")
    void isolatesConcurrentRequestContexts() throws Exception {
        AuthorizationRequestSubjectContext firstContext = new AuthorizationRequestSubjectContext();
        AuthorizationRequestSubjectContext secondContext = new AuthorizationRequestSubjectContext();
        SubjectAttributes firstSubject = subject(1L, EVALUATED_AT);
        SubjectAttributes secondSubject = subject(2L, EVALUATED_AT.plusSeconds(1));
        List<Callable<SubjectAttributes>> tasks = List.of(
            () -> firstContext.getOrLoad(1L, ignored -> firstSubject),
            () -> secondContext.getOrLoad(2L, ignored -> secondSubject)
        );

        try (var executor = Executors.newFixedThreadPool(2)) {
            List<SubjectAttributes> results = executor.invokeAll(tasks).stream()
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception exception) {
                        throw new AssertionError(exception);
                    }
                })
                .toList();

            assertThat(results).containsExactlyInAnyOrder(firstSubject, secondSubject);
            assertThat(firstContext.require(1L)).isSameAs(firstSubject);
            assertThat(secondContext.require(2L)).isSameAs(secondSubject);
        }
    }

    private static SubjectAttributes subject(long memberId, Instant evaluatedAt) {
        return new SubjectPolicyFacts(evaluatedAt, List.of(), List.of(), Map.of())
            .toSubjectAttributes(memberId, 10L);
    }
}
