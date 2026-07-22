package com.umc.product.notification.adapter.in.aop;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.annotation.WebhookAlarm;
import com.umc.product.notification.domain.WebhookPlatform;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookAlarmAspect")
class WebhookAlarmAspectTest {

    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    @Mock
    JoinPoint joinPoint;
    @Mock
    MethodSignature methodSignature;

    @Test
    @DisplayName("메서드 인자와 반환값으로 SpEL을 평가해 즉시 전송한다")
    void spel을_평가해_즉시_전송한다() throws Exception {
        WebhookAlarm annotation = prepare("immediate", "회원", 42L);
        WebhookAlarmAspect sut = new WebhookAlarmAspect(sendWebhookAlarmUseCase);

        sut.sendAlarm(joinPoint, annotation, 42L);

        then(sendWebhookAlarmUseCase).should().send(argThat(command ->
            command.platforms().equals(java.util.List.of(WebhookPlatform.SLACK))
                && command.title().equals("완료: 회원")
                && command.content().equals("ID: 42")
        ));
        then(sendWebhookAlarmUseCase).should(never()).sendBuffered(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("buffered 설정이면 평가한 명령을 이벤트 발행 경로로 전달한다")
    void buffered_설정이면_이벤트_경로로_전달한다() throws Exception {
        WebhookAlarm annotation = prepare("buffered", "공지", "created");
        WebhookAlarmAspect sut = new WebhookAlarmAspect(sendWebhookAlarmUseCase);

        sut.sendAlarm(joinPoint, annotation, "created");

        then(sendWebhookAlarmUseCase).should().sendBuffered(argThat(command ->
            command.platforms().equals(java.util.List.of(WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD))
                && command.title().equals("공지")
                && command.content().equals("created")
        ));
        then(sendWebhookAlarmUseCase).should(never()).send(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("잘못된 SpEL은 원래 메서드의 성공 흐름에 예외를 전파하지 않는다")
    void 잘못된_spel은_예외를_전파하지_않는다() throws Exception {
        WebhookAlarm annotation = prepare("invalid", "입력", "결과");
        WebhookAlarmAspect sut = new WebhookAlarmAspect(sendWebhookAlarmUseCase);

        sut.sendAlarm(joinPoint, annotation, "결과");

        then(sendWebhookAlarmUseCase).shouldHaveNoInteractions();
    }

    private WebhookAlarm prepare(String methodName, Object argument, Object result) throws Exception {
        Method method = SampleMethods.class.getDeclaredMethod(methodName, String.class);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.toShortString()).willReturn("SampleMethods." + methodName + "(..)");
        given(methodSignature.getMethod()).willReturn(method);
        given(joinPoint.getArgs()).willReturn(new Object[]{argument});
        return method.getAnnotation(WebhookAlarm.class);
    }

    private static class SampleMethods {

        @WebhookAlarm(
            platforms = WebhookPlatform.SLACK,
            title = "'완료: ' + #p0",
            content = "'ID: ' + #result"
        )
        void immediate(String name) {
        }

        @WebhookAlarm(
            title = "#p0",
            content = "#result",
            buffered = true
        )
        void buffered(String title) {
        }

        @WebhookAlarm(
            title = "#missing.value",
            content = "#result"
        )
        void invalid(String value) {
        }
    }
}
