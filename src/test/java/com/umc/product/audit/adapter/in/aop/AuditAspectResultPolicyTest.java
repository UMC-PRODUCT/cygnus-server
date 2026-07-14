package com.umc.product.audit.adapter.in.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.event.domain.DomainEvent;
import com.umc.product.global.exception.constant.Domain;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditAspectResultPolicyTest {

    @Mock
    DomainEventPublisher eventPublisher;
    @Mock
    JoinPoint joinPoint;
    @Mock
    MethodSignature methodSignature;
    @Mock
    Audited audited;

    AuditAspect sut;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        sut = new AuditAspect(eventPublisher);
        Method method = AuditAspectResultPolicyTest.class.getDeclaredMethod("auditedMethod");
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(joinPoint.getArgs()).willReturn(new Object[0]);
        given(methodSignature.getMethod()).willReturn(method);
        given(audited.domain()).willReturn(Domain.PROJECT);
        given(audited.action()).willReturn(AuditAction.FINALIZE);
        given(audited.targetType()).willReturn("ProjectMatchingRound");
        given(audited.targetId()).willReturn("");
        given(audited.description()).willReturn("");
    }

    @Test
    @DisplayName("기본 감사 정책은 false 결과도 기존처럼 발행한다")
    void defaultPolicyPublishesFalseResult() {
        given(audited.publishOnTrueResultOnly()).willReturn(false);

        sut.publishAuditEvent(joinPoint, audited, false);

        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        then(eventPublisher).should().publish(event.capture());
        assertThat(event.getValue()).isInstanceOf(AuditLogEvent.class);
    }

    @Test
    @DisplayName("기본 감사 정책은 void 결과도 기존처럼 발행한다")
    void defaultPolicyPublishesVoidResult() {
        given(audited.publishOnTrueResultOnly()).willReturn(false);

        sut.publishAuditEvent(joinPoint, audited, null);

        then(eventPublisher).should().publish(org.mockito.ArgumentMatchers.any(DomainEvent.class));
    }

    @Test
    @DisplayName("true 결과 전용 감사 정책은 true일 때만 발행한다")
    void trueOnlyPolicyPublishesTrueResult() {
        given(audited.publishOnTrueResultOnly()).willReturn(true);

        sut.publishAuditEvent(joinPoint, audited, true);

        then(eventPublisher).should().publish(org.mockito.ArgumentMatchers.any(DomainEvent.class));
    }

    @Test
    @DisplayName("true 결과 전용 감사 정책은 false no-op을 발행하지 않는다")
    void trueOnlyPolicySkipsFalseResult() {
        given(audited.publishOnTrueResultOnly()).willReturn(true);

        sut.publishAuditEvent(joinPoint, audited, false);

        then(eventPublisher).should(never()).publish(org.mockito.ArgumentMatchers.any(DomainEvent.class));
    }

    private static boolean auditedMethod() {
        return true;
    }
}
