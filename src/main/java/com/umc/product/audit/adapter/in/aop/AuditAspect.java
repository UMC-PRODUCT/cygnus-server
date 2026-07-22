package com.umc.product.audit.adapter.in.aop;

import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.security.MemberPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Audited} 어노테이션이 붙은 메서드 실행 성공 후 감사 로그 이벤트를 발행합니다.
 * <p>
 * 기존 {@code WebhookAlarmAspect} 패턴을 따릅니다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final DomainEventPublisher eventPublisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void publishAuditEvent(JoinPoint joinPoint, Audited audited, Object result) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);

            String targetId = evaluateSpel(audited.targetId(), context);
            String description = evaluateSpel(audited.description(), context);
            Long actorMemberId = extractMemberId();
            String ipAddress = extractIpAddress();

            AuditLogEvent event = AuditLogEvent.builder()
                .domain(audited.domain())
                .action(audited.action())
                .targetType(audited.targetType())
                .targetId(targetId)
                .actorMemberId(actorMemberId)
                .description(description)
                .details(Map.of())
                .ipAddress(ipAddress)
                .build();

            eventPublisher.publish(event);
        } catch (Exception e) {
            log.error("감사 로그 이벤트 발행 중 오류: method={}, error={}",
                joinPoint.getSignature().toShortString(), e.getMessage(), e);
        }
    }

    private EvaluationContext createEvaluationContext(JoinPoint joinPoint, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
            null,
            signature.getMethod(),
            joinPoint.getArgs(),
            parameterNameDiscoverer
        );
        context.setVariable("result", result);
        return context;
    }

    private String evaluateSpel(String expression, EvaluationContext context) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }
        Object value = parser.parseExpression(expression).getValue(context);
        return value != null ? value.toString() : null;
    }

    private Long extractMemberId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof MemberPrincipal principal) {
                return principal.getMemberId();
            }
        } catch (Exception e) {
            log.debug("SecurityContext에서 memberId 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("IP 주소 추출 실패: {}", e.getMessage());
            return null;
        }
        return null;
    }
}
