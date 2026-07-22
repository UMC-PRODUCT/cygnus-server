package com.umc.product.audit.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.umc.product.audit.adapter.in.aop.AuditAspect;
import com.umc.product.audit.adapter.in.event.AuditLogEventListener;
import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.application.port.in.command.SaveAuditLogUseCase;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;

@DisplayName("감사 로그 입력 adapter 잔여 계약")
class AuditInputAdapterResidualTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("감사 annotation의 인자·반환값과 인증 회원·프록시 IP를 이벤트로 발행한다")
    void 감사_문맥을_이벤트로_발행한다() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuditAspect sut = new AuditAspect(publisher);
        Method method = Fixture.class.getDeclaredMethod("update", String.class);
        JoinPoint joinPoint = joinPoint(method, "회원 이름");
        Audited audited = method.getAnnotation(Audited.class);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(10L), null));
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getHeader("X-Forwarded-For")).willReturn(" 203.0.113.10, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sut.publishAuditEvent(joinPoint, audited, 20L);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        then(publisher).should().publish(captor.capture());
        assertThat(captor.getValue())
            .returns(Domain.MEMBER, AuditLogEvent::domain)
            .returns(AuditAction.UPDATE, AuditLogEvent::action)
            .returns("Member", AuditLogEvent::targetType)
            .returns("20", AuditLogEvent::targetId)
            .returns(10L, AuditLogEvent::actorMemberId)
            .returns("변경: 회원 이름", AuditLogEvent::description)
            .returns("203.0.113.10", AuditLogEvent::ipAddress);
    }

    @Test
    @DisplayName("빈 표현식·비회원 인증·프록시 헤더 부재는 null과 원격 IP로 안전하게 처리한다")
    void 선택_문맥_부재를_안전하게_처리한다() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuditAspect sut = new AuditAspect(publisher);
        Method method = Fixture.class.getDeclaredMethod("delete");
        JoinPoint joinPoint = joinPoint(method);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymous", null));
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRemoteAddr()).willReturn("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        sut.publishAuditEvent(joinPoint, method.getAnnotation(Audited.class), null);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        then(publisher).should().publish(captor.capture());
        assertThat(captor.getValue().targetId()).isNull();
        assertThat(captor.getValue().description()).isNull();
        assertThat(captor.getValue().actorMemberId()).isNull();
        assertThat(captor.getValue().ipAddress()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("HTTP 요청 밖에서 실행되는 비동기 업무는 IP 없이 감사 이벤트를 발행한다")
    void HTTP_요청_문맥이_없으면_IP를_생략한다() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuditAspect sut = new AuditAspect(publisher);
        Method method = Fixture.class.getDeclaredMethod("delete");

        sut.publishAuditEvent(joinPoint(method), method.getAnnotation(Audited.class), null);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        then(publisher).should().publish(captor.capture());
        assertThat(captor.getValue().ipAddress()).isNull();
    }

    @Test
    @DisplayName("SecurityContext와 RequestContext 접근 실패도 감사 이벤트 발행과 본 업무를 방해하지 않는다")
    void 문맥_조회_실패를_격리한다() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuditAspect sut = new AuditAspect(publisher);
        Method method = Fixture.class.getDeclaredMethod("delete");
        JoinPoint joinPoint = joinPoint(method);
        SecurityContext brokenSecurityContext = mock(SecurityContext.class);
        given(brokenSecurityContext.getAuthentication()).willThrow(new IllegalStateException("security fail"));
        SecurityContextHolder.setContext(brokenSecurityContext);
        HttpServletRequest brokenRequest = mock(HttpServletRequest.class);
        given(brokenRequest.getHeader("X-Forwarded-For")).willThrow(new IllegalStateException("request fail"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(brokenRequest));

        sut.publishAuditEvent(joinPoint, method.getAnnotation(Audited.class), null);

        ArgumentCaptor<AuditLogEvent> captor = ArgumentCaptor.forClass(AuditLogEvent.class);
        then(publisher).should().publish(captor.capture());
        assertThat(captor.getValue().actorMemberId()).isNull();
        assertThat(captor.getValue().ipAddress()).isNull();
    }

    @Test
    @DisplayName("잘못된 SpEL은 예외를 외부로 전파하거나 불완전한 이벤트를 발행하지 않는다")
    void 잘못된_SpEL을_격리한다() throws Exception {
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        AuditAspect sut = new AuditAspect(publisher);
        Method method = Fixture.class.getDeclaredMethod("invalid");

        sut.publishAuditEvent(joinPoint(method), method.getAnnotation(Audited.class), null);

        then(publisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이벤트 listener는 저장을 위임하고 저장 실패를 발행자에게 전파하지 않는다")
    void 이벤트_저장과_실패를_격리한다() {
        SaveAuditLogUseCase saveUseCase = mock(SaveAuditLogUseCase.class);
        AuditLogEventListener sut = new AuditLogEventListener(saveUseCase);
        AuditLogEvent first = event("1");
        AuditLogEvent second = event("2");
        org.mockito.Mockito.doThrow(new IllegalStateException("db fail")).when(saveUseCase).save(second);

        sut.handle(first);
        sut.handle(second);

        then(saveUseCase).should().save(first);
        then(saveUseCase).should().save(second);
    }

    private JoinPoint joinPoint(Method method, Object... args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(joinPoint.getArgs()).willReturn(args);
        given(signature.getMethod()).willReturn(method);
        given(signature.toShortString()).willReturn(method.getName());
        return joinPoint;
    }

    private AuditLogEvent event(String targetId) {
        return AuditLogEvent.builder()
            .domain(Domain.MEMBER)
            .action(AuditAction.UPDATE)
            .targetType("Member")
            .targetId(targetId)
            .build();
    }

    private static final class Fixture {

        @Audited(
            domain = Domain.MEMBER,
            action = AuditAction.UPDATE,
            targetType = "Member",
            targetId = "#result",
            description = "'변경: ' + #value"
        )
        Long update(String value) {
            return 20L;
        }

        @Audited(domain = Domain.MEMBER, action = AuditAction.DELETE, targetType = "Member")
        void delete() {
        }

        @Audited(
            domain = Domain.MEMBER,
            action = AuditAction.UPDATE,
            targetType = "Member",
            targetId = "#"
        )
        void invalid() {
        }
    }
}
