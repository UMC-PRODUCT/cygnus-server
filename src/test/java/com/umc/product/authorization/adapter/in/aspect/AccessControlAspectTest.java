package com.umc.product.authorization.adapter.in.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.lang.reflect.Method;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.audit.application.port.in.command.dto.RecordAuditLogCommand;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessControlAspect 실패 감사 로그 계약")
class AccessControlAspectTest {

    private static final Long MEMBER_ID = 42L;
    private static final Long RESOURCE_ID = 9001L;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @Mock
    RecordAuditLogUseCase recordAuditLogUseCase;

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    MethodSignature methodSignature;

    @InjectMocks
    AccessControlAspect aspect;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증된 사용자의 접근 거부를 리소스와 권한만 포함해 기록하고 원 예외를 유지한다")
    void authenticatedDenialIsRecordedAndOriginalExceptionIsPreserved() throws Throwable {
        // given
        authenticate(MEMBER_ID);
        CheckAccess checkAccess = configureJoinPoint("edit", RESOURCE_ID);
        given(checkPermissionUseCase.check(eq(MEMBER_ID), any(ResourcePermission.class)))
            .willReturn(false);

        // when
        Throwable thrown = catchThrowable(() -> aspect.checkAccess(joinPoint, checkAccess));

        // then
        assertThat(thrown)
            .isInstanceOf(AuthorizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);

        RecordAuditLogCommand recorded = captureSingleRecord();
        assertAccessDenied(recorded, MEMBER_ID, RESOURCE_ID.toString());
    }

    @Test
    @DisplayName("익명 사용자의 접근 거부도 actor 없이 기록하고 기존 AccessDeniedException을 유지한다")
    void anonymousDenialIsRecordedWithoutActor() throws Throwable {
        // given
        CheckAccess checkAccess = configureJoinPoint("edit", RESOURCE_ID);

        // when
        Throwable thrown = catchThrowable(() -> aspect.checkAccess(joinPoint, checkAccess));

        // then
        assertThat(thrown).isInstanceOf(AccessDeniedException.class);
        RecordAuditLogCommand recorded = captureSingleRecord();
        assertAccessDenied(recorded, null, RESOURCE_ID.toString());
    }

    @Test
    @DisplayName("반복 접근 거부는 매번 감사 기록한다")
    void repeatedDenialsAreRecordedIndividually() throws Throwable {
        // given
        authenticate(MEMBER_ID);
        CheckAccess checkAccess = configureJoinPoint("edit", RESOURCE_ID);
        given(checkPermissionUseCase.check(eq(MEMBER_ID), any(ResourcePermission.class)))
            .willReturn(false);

        // when
        catchThrowable(() -> aspect.checkAccess(joinPoint, checkAccess));
        catchThrowable(() -> aspect.checkAccess(joinPoint, checkAccess));

        // then
        then(recordAuditLogUseCase).should(times(2)).record(any(RecordAuditLogCommand.class));
    }

    @Test
    @DisplayName("감사 기록기가 실패해도 원 인가 예외를 유지한다")
    void recorderFailureDoesNotReplaceAuthorizationFailure() throws Throwable {
        // given
        authenticate(MEMBER_ID);
        CheckAccess checkAccess = configureJoinPoint("edit", RESOURCE_ID);
        given(checkPermissionUseCase.check(eq(MEMBER_ID), any(ResourcePermission.class)))
            .willReturn(false);
        doThrow(new IllegalStateException("audit-recorder-failure"))
            .when(recordAuditLogUseCase)
            .record(any(RecordAuditLogCommand.class));

        // when
        Throwable thrown = catchThrowable(() -> aspect.checkAccess(joinPoint, checkAccess));

        // then
        assertThat(thrown)
            .isInstanceOf(AuthorizationDomainException.class)
            .extracting("baseCode")
            .isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);
        then(recordAuditLogUseCase).should().record(any(RecordAuditLogCommand.class));
    }

    @Test
    @DisplayName("접근 허용은 감사 실패 로그 없이 원 메서드를 실행한다")
    void allowedAccessProceedsWithoutFailureAudit() throws Throwable {
        // given
        authenticate(MEMBER_ID);
        CheckAccess checkAccess = configureJoinPoint("edit", RESOURCE_ID);
        given(checkPermissionUseCase.check(eq(MEMBER_ID), any(ResourcePermission.class)))
            .willReturn(true);
        given(joinPoint.proceed()).willReturn("allowed");

        // when
        Object result = aspect.checkAccess(joinPoint, checkAccess);

        // then
        assertThat(result).isEqualTo("allowed");
        then(recordAuditLogUseCase).should(never()).record(any(RecordAuditLogCommand.class));
    }

    private CheckAccess configureJoinPoint(String methodName, Object resourceId) throws Exception {
        CheckAccess checkAccess = checkAccess(methodName);
        given(joinPoint.getSignature()).willReturn(methodSignature);
        given(methodSignature.getParameterNames()).willReturn(new String[]{"resourceId"});
        given(joinPoint.getArgs()).willReturn(new Object[]{resourceId});
        return checkAccess;
    }

    private CheckAccess checkAccess(String methodName) throws Exception {
        Method method = SecuredOperation.class.getDeclaredMethod(
            methodName,
            methodName.equals("edit") ? new Class<?>[]{Long.class} : new Class<?>[]{}
        );
        return method.getAnnotation(CheckAccess.class);
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
            )
        );
    }

    private RecordAuditLogCommand captureSingleRecord() {
        ArgumentCaptor<RecordAuditLogCommand> captor =
            ArgumentCaptor.forClass(RecordAuditLogCommand.class);
        then(recordAuditLogUseCase).should().record(captor.capture());
        return captor.getValue();
    }

    private void assertAccessDenied(
        RecordAuditLogCommand recorded,
        Long actorMemberId,
        String resourceId
    ) {
        assertThat(recorded.domain()).isEqualTo(Domain.AUTHORIZATION);
        assertThat(recorded.action()).isEqualTo(AuditAction.ACCESS_DENIED);
        assertThat(recorded.targetType()).isEqualTo(ResourceType.SCHEDULE.name());
        assertThat(recorded.targetId()).isEqualTo(resourceId);
        assertThat(recorded.actorMemberId()).isEqualTo(actorMemberId);
        assertThat(recorded.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(recorded.source()).isEqualTo(AuditSource.AUTHORIZATION_ASPECT);
        assertThat(recorded.details()).containsEntry("schemaVersion", 1);
        assertThat(recorded.details().get("actor"))
            .isEqualTo(actorMemberId == null ? Map.of() : Map.of("memberId", actorMemberId));
        assertThat(recorded.details().get("target"))
            .isEqualTo(resourceId == null
                ? Map.of("type", ResourceType.SCHEDULE.name())
                : Map.of("type", ResourceType.SCHEDULE.name(), "id", resourceId));
        assertThat(recorded.details().get("context"))
            .isEqualTo(Map.of("permission", PermissionType.EDIT.name()));
    }

    private interface SecuredOperation {

        @CheckAccess(
            resourceType = ResourceType.SCHEDULE,
            resourceId = "#resourceId",
            permission = PermissionType.EDIT,
            message = "수정 권한이 없습니다."
        )
        void edit(Long resourceId);

        @CheckAccess(
            resourceType = ResourceType.SCHEDULE,
            permission = PermissionType.EDIT,
            message = "조회 권한이 없습니다."
        )
        void readAll();
    }
}
