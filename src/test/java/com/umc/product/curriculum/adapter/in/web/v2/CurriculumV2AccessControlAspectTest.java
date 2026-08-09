package com.umc.product.curriculum.adapter.in.web.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.authorization.adapter.in.aspect.AccessControlAspect;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.curriculum.adapter.in.web.v2.dto.request.CreateBestWorkbookRequest;
import com.umc.product.global.security.MemberPrincipal;

class CurriculumV2AccessControlAspectTest {

    private static final Long MEMBER_ID = 99L;
    private static final MemberPrincipal PRINCIPAL = MemberPrincipal.builder().memberId(MEMBER_ID).build();

    private CheckPermissionUseCase checkPermissionUseCase;
    private AccessControlAspect aspect;

    @BeforeEach
    void setUp() {
        checkPermissionUseCase = mock(CheckPermissionUseCase.class);
        aspect = new AccessControlAspect(checkPermissionUseCase);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(PRINCIPAL, null, PRINCIPAL.getAuthorities())
        );
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "{0} 권한 사용자는 요청을 진행한다")
    @MethodSource("curriculumResources")
    void authorizedUser_proceeds(
        String name,
        Method method,
        String[] parameterNames,
        Object[] arguments,
        ResourcePermission expectedPermission
    ) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(method, parameterNames, arguments);
        given(checkPermissionUseCase.check(MEMBER_ID, expectedPermission)).willReturn(true);
        given(joinPoint.proceed()).willReturn("ok");

        Object result = aspect.checkAccess(joinPoint, method.getAnnotation(CheckAccess.class));

        assertThat(result).isEqualTo("ok");
        then(checkPermissionUseCase).should().check(MEMBER_ID, expectedPermission);
        then(joinPoint).should().proceed();
    }

    @ParameterizedTest(name = "{0} 비권한 사용자는 403 예외로 차단한다")
    @MethodSource("curriculumResources")
    void unauthorizedUser_isDenied(
        String name,
        Method method,
        String[] parameterNames,
        Object[] arguments,
        ResourcePermission expectedPermission
    ) throws Throwable {
        ProceedingJoinPoint joinPoint = joinPoint(method, parameterNames, arguments);
        given(checkPermissionUseCase.check(MEMBER_ID, expectedPermission)).willReturn(false);

        assertThatThrownBy(() ->
            aspect.checkAccess(joinPoint, method.getAnnotation(CheckAccess.class))
        ).isInstanceOf(AuthorizationDomainException.class);

        then(joinPoint).should(never()).proceed();
    }

    private static Stream<Arguments> curriculumResources() throws NoSuchMethodException {
        CreateBestWorkbookRequest bestRequest = new CreateBestWorkbookRequest(10L, 20L, 30L, "사유");
        return Stream.of(
            Arguments.of(
                "챌린저 워크북",
                ChallengerWorkbookCommandV2Controller.class.getDeclaredMethod(
                    "delete",
                    Long.class,
                    String.class,
                    MemberPrincipal.class
                ),
                new String[]{"challengerWorkbookId", "reason", "principal"},
                new Object[]{10L, "삭제 사유", PRINCIPAL},
                ResourcePermission.of(ResourceType.CHALLENGER_WORKBOOK, 10L, PermissionType.DELETE)
            ),
            Arguments.of(
                "주간 베스트 워크북",
                ChallengerWorkbookCommandV2Controller.class.getDeclaredMethod(
                    "selectBest",
                    CreateBestWorkbookRequest.class,
                    MemberPrincipal.class
                ),
                new String[]{"request", "principal"},
                new Object[]{bestRequest, PRINCIPAL},
                ResourcePermission.of(ResourceType.WEEKLY_BEST_WORKBOOK, 30L, PermissionType.WRITE)
            ),
            Arguments.of(
                "미션 제출물",
                ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
                    "editMissionSubmission",
                    MemberPrincipal.class,
                    Long.class,
                    String.class
                ),
                new String[]{"principal", "missionSubmissionId", "content"},
                new Object[]{PRINCIPAL, 40L, "수정"},
                ResourcePermission.of(ResourceType.MISSION_SUBMISSION, 40L, PermissionType.EDIT)
            ),
            Arguments.of(
                "미션 피드백",
                ChallengerWorkbookMissionCommandV2Controller.class.getDeclaredMethod(
                    "editMissionFeedback",
                    MemberPrincipal.class,
                    Long.class,
                    String.class
                ),
                new String[]{"principal", "missionFeedbackId", "content"},
                new Object[]{PRINCIPAL, 50L, "수정"},
                ResourcePermission.of(ResourceType.MISSION_FEEDBACK, 50L, PermissionType.EDIT)
            )
        );
    }

    private ProceedingJoinPoint joinPoint(
        Method method,
        String[] parameterNames,
        Object[] arguments
    ) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        given(joinPoint.getSignature()).willReturn(signature);
        given(signature.getParameterNames()).willReturn(parameterNames);
        given(joinPoint.getArgs()).willReturn(arguments);
        return joinPoint;
    }
}
