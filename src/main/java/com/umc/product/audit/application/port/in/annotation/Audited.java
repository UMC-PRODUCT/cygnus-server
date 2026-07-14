package com.umc.product.audit.application.port.in.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.global.exception.constant.Domain;

/**
 * 메서드 실행 성공 후 감사 로그를 자동 기록하는 어노테이션
 *
 * <p>사용 예시:</p>
 * <pre>{@code
 * @Audited(
 *     domain = Domain.SCHEDULE,
 *     action = AuditAction.CREATE,
 *     targetType = "Schedule",
 *     targetId = "#result",
 *     description = "'일정 생성: ' + #command.name()"
 * )
 * public Long create(CreateScheduleCommand command) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    Domain domain();

    AuditAction action();

    /**
     * 대상 엔티티 타입명 (e.g., "Schedule", "Member")
     */
    String targetType();

    /**
     * 대상 엔티티 ID (SpEL 표현식)
     * <p>
     * - 메서드 파라미터 참조: {@code "#command.scheduleId()"} - 반환값 참조: {@code "#result"} - 빈 문자열이면 null로 저장
     */
    String targetId() default "";

    /**
     * 사람이 읽을 수 있는 설명 (SpEL 표현식)
     */
    String description() default "";

    boolean publishOnTrueResultOnly() default false;
}
