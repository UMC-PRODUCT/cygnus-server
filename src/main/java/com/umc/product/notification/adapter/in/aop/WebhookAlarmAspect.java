package com.umc.product.notification.adapter.in.aop;

import java.util.List;

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
import org.springframework.stereotype.Component;

import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.annotation.WebhookAlarm;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class WebhookAlarmAspect {

    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning(pointcut = "@annotation(webhookAlarm)", returning = "result")
    public void sendAlarm(JoinPoint joinPoint, WebhookAlarm webhookAlarm, Object result) {
        log.debug("@WebhookAlarm 감지: method={}", joinPoint.getSignature().toShortString());

        try {
            EvaluationContext context = createEvaluationContext(joinPoint, result);

            String title = parser.parseExpression(webhookAlarm.title()).getValue(context, String.class);
            String content = parser.parseExpression(webhookAlarm.content()).getValue(context, String.class);
            List<WebhookPlatform> platforms = List.of(webhookAlarm.platforms());

            SendWebhookAlarmCommand command = new SendWebhookAlarmCommand(platforms, title, content);

            if (webhookAlarm.buffered()) {
                sendWebhookAlarmUseCase.sendBuffered(command);
            } else {
                sendWebhookAlarmUseCase.send(command);
            }
        } catch (Exception e) {
            log.warn("웹훅 알람 처리 중 오류: method={}, error={}",
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
}
