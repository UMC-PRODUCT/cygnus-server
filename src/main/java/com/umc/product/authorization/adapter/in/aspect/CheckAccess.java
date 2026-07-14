package com.umc.product.authorization.adapter.in.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;

/**
 * 메서드 실행 전 권한 체크를 수행하는 어노테이션
 *
 * <p>사용 예시:</p>
 * <pre>
 * {@code
 * @CheckAccess(
 *     resourceType = ResourceType.CURRICULUM,
 *     resourceId = "#workbookId",
 *     permission = PermissionType.WRITE
 * )
 * public void submitWorkbook(Long workbookId, String content) {
 *     // ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckAccess {

    /**
     * 리소스 타입 (CURRICULUM, SCHEDULE, NOTICE 등)
     */
    ResourceType resourceType();

    /**
     * 리소스 ID (SpEL 표현식 지원)
     * <p>
     * - 메서드 파라미터 참조: {@code #paramName} - 객체 필드 접근: {@code #request.id} - 리소스 타입 전체에 대한 권한 체크 시: {@code ""}
     * </p>
     */
    String resourceId() default "";

    /**
     * 필요한 권한
     */
    PermissionType permission();

    String action() default "";

    /**
     * 권한이 없을 때 표시할 메시지
     */
    String message() default "이 항목에 접근할 권한이 없어요. 필요한 권한이 있다면 운영진에게 문의해주세요.";
}
