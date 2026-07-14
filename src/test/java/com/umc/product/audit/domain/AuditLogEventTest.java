package com.umc.product.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.global.event.domain.OutboxDispatchMode;
import com.umc.product.global.exception.constant.Domain;

class AuditLogEventTest {

    @Test
    @DisplayName("eventId와 occurredAt을 지정하지 않으면 기본값이 자동 주입된다")
    void 메타데이터_미지정시_기본값_자동주입() {
        // given
        Instant before = Instant.now();

        // when
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.REGISTER)
            .targetType("Member")
            .targetId("1")
            .build();

        // then
        Instant after = Instant.now();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isBetween(before, after);
        assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(event.source()).isEqualTo(AuditSource.ANNOTATION);
        assertThat(event.requestId()).isNull();
        assertThat(event.traceId()).isNull();
    }

    @Test
    @DisplayName("감사 결과와 출처 enum은 저장 계약의 값만 제공한다")
    void 감사_결과와_출처_enum은_저장_계약의_값만_제공한다() {
        // when & then
        assertThat(Arrays.asList(AuditOutcome.values()))
            .containsExactly(AuditOutcome.SUCCESS, AuditOutcome.FAILURE);
        assertThat(Arrays.asList(AuditSource.values()))
            .containsExactly(
                AuditSource.ANNOTATION,
                AuditSource.EXPLICIT_RECORDER,
                AuditSource.AUTHENTICATION_SERVICE,
                AuditSource.AUTHORIZATION_ASPECT,
                AuditSource.SYSTEM
            );
    }

    @Test
    @DisplayName("감사 결과와 출처 및 요청 추적값을 명시하면 그대로 유지한다")
    void 감사_결과와_출처_및_요청_추적값을_명시하면_그대로_유지한다() {
        // given
        String requestId = "request-123";
        String traceId = "0123456789abcdef0123456789abcdef";

        // when
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN)
            .targetType("Authentication")
            .outcome(AuditOutcome.FAILURE)
            .source(AuditSource.AUTHENTICATION_SERVICE)
            .requestId(requestId)
            .traceId(traceId)
            .build();

        // then
        assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(event.source()).isEqualTo(AuditSource.AUTHENTICATION_SERVICE);
        assertThat(event.requestId()).isEqualTo(requestId);
        assertThat(event.traceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("eventId와 occurredAt을 명시하면 그 값이 그대로 유지된다")
    void 메타데이터_명시시_값_유지() {
        // given
        UUID givenId = UUID.randomUUID();
        Instant givenInstant = Instant.parse("2026-01-01T00:00:00Z");

        // when
        AuditLogEvent event = AuditLogEvent.builder()
            .eventId(givenId)
            .occurredAt(givenInstant)
            .domain(Domain.MEMBER)
            .action(AuditAction.REGISTER)
            .targetType("Member")
            .targetId("1")
            .build();

        // then
        assertThat(event.eventId()).isEqualTo(givenId);
        assertThat(event.occurredAt()).isEqualTo(givenInstant);
    }

    @Test
    @DisplayName("eventType은 'audit.log.<action>' 형식으로 생성된다")
    void eventType은_action_기반으로_생성() {
        // given
        AuditLogEvent registerEvent = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.REGISTER)
            .targetType("Member")
            .targetId("1")
            .build();
        AuditLogEvent withdrawEvent = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.WITHDRAW)
            .targetType("Member")
            .targetId("1")
            .build();

        // then
        assertThat(registerEvent.eventType()).isEqualTo("audit.log.register");
        assertThat(withdrawEvent.eventType()).isEqualTo("audit.log.withdraw");
    }

    @Test
    @DisplayName("감사 이벤트는 relay가 저장 결과를 확인할 수 있도록 non-transactional dispatch를 사용한다")
    void 감사_이벤트는_non_transactional_dispatch를_사용한다() {
        AuditLogEvent event = AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId("1")
            .build();

        assertThat(event.outboxDispatchMode()).isEqualTo(OutboxDispatchMode.NON_TRANSACTIONAL);
    }
}
