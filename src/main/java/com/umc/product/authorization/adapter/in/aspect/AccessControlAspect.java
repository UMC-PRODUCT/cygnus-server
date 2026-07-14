package com.umc.product.authorization.adapter.in.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.global.security.MemberPrincipal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link CheckAccess} 어노테이션이 붙은 메서드 실행 전 권한을 체크하는 Aspect
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AccessControlAspect {

    private final CheckPermissionUseCase checkPermissionUseCase;
    private final AuthorizationRequestSubjectContext requestSubjectContext;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(checkAccess)")
    public Object checkAccess(ProceedingJoinPoint joinPoint, CheckAccess checkAccess) throws Throwable {
        // 1. 인증된 사용자 추출
        Long memberId = extractMemberId();

        // 2. SpEL로 resourceId 평가
        // checkAccess 어노테이션을 사용할 때 SpEL 표현식으로 작성한 resourceId의 값을 가져오면 됨.
        // resourceType은 별도로 checkAccess 어노테이션에서 명시해줌.
        String resourceId = evaluateResourceId(joinPoint, checkAccess.resourceId());

        // 3. ResourcePermission 생성
        ResourcePermission permission = resourceId == null || resourceId.isEmpty()
                // resourceId가 명시되어 있지 않은 경우 (리소스 타입 전체에 대한 권한 체크)
                ? ResourcePermission.ofType(checkAccess.resourceType(), checkAccess.permission())
                // resourceId가 명시된 경우 (특정 리소스에 대한 권한 체크)
                : ResourcePermission.of(checkAccess.resourceType(), resourceId, checkAccess.permission());

        // 4. 권한 체크
        SubjectAttributes subject = requestSubjectContext.getOrLoad(
            memberId,
            checkPermissionUseCase::loadSubject
        );
        boolean hasAccess = checkAccess.action().isBlank()
            ? checkPermissionUseCase.check(subject, permission)
            : checkPermissionUseCase.check(subject, permission, checkAccess.action());

        if (!hasAccess) {
            log.warn("요청한 리소스에 접근 권한이 없습니다. - memberId: {}, resource: {}:{}, permission: {}",
                    memberId,
                    checkAccess.resourceType(),
                    resourceId,
                    checkAccess.permission());

            throw new AuthorizationDomainException(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED,
                    checkAccess.message());
        }

        // 5. 권한 있으면 원래 메서드 실행
        return joinPoint.proceed();
    }

    /**
     * SecurityContext에서 인증된 사용자의 ID 추출
     */
    private Long extractMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("로그인이 필요해요. 로그인 후 다시 시도해주세요.");
        }

        // MemberPrincipal에서 memberId 추출
        if (authentication.getPrincipal() instanceof MemberPrincipal principal) {
            return principal.getMemberId();
        }

        // TODO: 이거 왜 여기서 걸림???????????
        // 인증 정보 없을 때는 76번 라인에서 걸려야되잖아;;;;;;;;;;

        // 근데 일단 Public API에 Permission check를 할 필요가 없으니까 나아아중에 강1이 고칠거임
        // https://www.youtube.com/shorts/p0pGZqC-wUU ; fuck!!!
        throw new AccessDeniedException("인증 정보가 올바르지 않아요. 다시 로그인해주세요.");
    }

    /**
     * SpEL 표현식으로 resourceId 평가
     * <p>
     * 평가라 함은, 표현식을 해석해서 그 실제 값을 얻어내는 것
     *
     * @param joinPoint  AOP JoinPoint
     * @param expression SpEL 표현식 (e.g., "#workbookId", "#request.id")
     * @return 평가된 resourceId (표현식이 비어있으면 null)
     */
    private String evaluateResourceId(ProceedingJoinPoint joinPoint, String expression) {
        // SpEL 표현식이 비어있으면 평가할 필요가 없음
        if (expression == null || expression.isEmpty()) {
            return null;
        }

        // SpEL 평가를 위한 컨텍스트 설정
        StandardEvaluationContext context = new StandardEvaluationContext();

        // joinPoint는 AOP가 가로챈 method에 대한 정보를 담고 있음
        // 아래 코드는 해당 method의 파라미터명과, 전달된 값을 context에 담고자 하는 것

        // 메서드 파라미터 이름과 값 매핑
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // method의 파라미터 이름들 (변수명)
        String[] parameterNames = signature.getParameterNames();
        // method에 전달된 실제 argument 값
        Object[] args = joinPoint.getArgs();

        // 파라미터가 여러 개일 때 묶어주는 용도
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        // SpEL 표현식 평가
        // parser는 SpEL 표현식 파서 (상단에 정의되어 있음)
        Expression exp = parser.parseExpression(expression);

        // SpEL에 있는 값을 context에서 찾아라!
        Object value = exp.getValue(context);

        return value != null ? value.toString() : null;
    }
}
