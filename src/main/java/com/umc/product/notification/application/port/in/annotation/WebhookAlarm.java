package com.umc.product.notification.application.port.in.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.umc.product.notification.domain.WebhookPlatform;

/**
 * 메서드 실행 성공 후 웹훅 알람을 전송하는 선언형 어노테이션.
 *
 * <p>{@code title}과 {@code content}는 SpEL(Spring Expression Language)로 평가됩니다.
 * 메서드 파라미터는 {@code #파라미터명}으로, 반환값은 {@code #result}로 참조할 수 있습니다.</p>
 *
 * <pre>
 * &#64;WebhookAlarm(
 *     platforms = {WebhookPlatform.SLACK, WebhookPlatform.DISCORD},
 *     title = "'챌린저 등록'",
 *     content = "'새 챌린저가 등록되었습니다. ID: ' + #result"
 * )
 * public Long register(RegisterChallengerCommand command) { ... }
 * </pre>
 *
 * <pre>
 * &#64;WebhookAlarm(
 *     platforms = WebhookPlatform.SLACK,
 *     title = "'공지사항 생성'",
 *     content = "'제목: ' + #command.title() + ', 작성자: ' + #command.authorName()",
 *     buffered = true
 * )
 * public Long createNotice(CreateNoticeCommand command) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface WebhookAlarm {

    /**
     * 알람을 전송할 플랫폼 목록.
     */
    WebhookPlatform[] platforms() default {WebhookPlatform.TELEGRAM, WebhookPlatform.DISCORD};

    /**
     * 알람 제목 (SpEL). 리터럴 문자열은 작은따옴표로 감싸야 합니다.
     * <p>예: {@code "'서버 알림'"} 또는 {@code "'알림: ' + #command.name()"}</p>
     */
    String title();

    /**
     * 알람 본문 (SpEL). 리터럴 문자열은 작은따옴표로 감싸야 합니다.
     * <p>예: {@code "'처리가 완료되었습니다.'"} 또는 {@code "'ID: ' + #result"}</p>
     */
    String content();

    /**
     * {@code true}이면 현재 흐름에서 즉시 전송하지 않고 도메인 이벤트로 발행하여 commit 이후 listener가 전송합니다. 기본값은 {@code false} (즉시 전송).
     */
    boolean buffered() default false;
}
