package com.umc.product.authorization.adapter.in.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.global.security.MemberPrincipal;

@ExtendWith(MockitoExtension.class)
@DisplayName("접근 제어 Aspect")
class AccessControlAspectTest {

    private static final Long MEMBER_ID = 17L;

    @Mock
    CheckPermissionUseCase checkPermissionUseCase;

    @Mock
    ProceedingJoinPoint joinPoint;

    @Mock
    MethodSignature methodSignature;

    @Mock
    CheckAccess checkAccess;

    AccessControlAspect sut;

    @BeforeEach
    void setUp() {
        sut = new AccessControlAspect(checkPermissionUseCase);
        org.mockito.Mockito.lenient()
            .when(checkAccess.resourceType())
            .thenReturn(ResourceType.CHALLENGER_ROLE);
        org.mockito.Mockito.lenient()
            .when(checkAccess.permission())
            .thenReturn(PermissionType.READ);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("인증 정보 추출")
    class AuthenticationContext {

        @Test
        @DisplayName("인증 정보가 없으면 권한 평가 전에 거부한다")
        void 인증_정보가_없으면_거부한다() {
            assertThatThrownBy(() -> sut.checkAccess(joinPoint, checkAccess))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("로그인이 필요");
            then(checkPermissionUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("인증되지 않은 토큰이면 권한 평가 전에 거부한다")
        void 인증되지_않은_토큰이면_거부한다() {
            Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
            given(authentication.isAuthenticated()).willReturn(false);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            assertThatThrownBy(() -> sut.checkAccess(joinPoint, checkAccess))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("로그인이 필요");
        }

        @Test
        @DisplayName("MemberPrincipal이 아닌 인증 주체이면 잘못된 인증으로 거부한다")
        void 다른_principal이면_거부한다() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymous", null, List.of())
            );

            assertThatThrownBy(() -> sut.checkAccess(joinPoint, checkAccess))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("인증 정보가 올바르지 않아요");
        }
    }

    @Nested
    @DisplayName("리소스 식별자 평가")
    class ResourceIdEvaluation {

        @BeforeEach
        void authenticate() {
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of())
            );
        }

        @Test
        @DisplayName("빈 표현식은 타입 전체 권한으로 평가한다")
        void 빈_표현식은_타입_전체_권한이다() throws Throwable {
            given(checkAccess.resourceId()).willReturn("");
            given(checkPermissionUseCase.check(
                MEMBER_ID,
                ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ)
            )).willReturn(true);
            given(joinPoint.proceed()).willReturn("allowed");

            Object result = sut.checkAccess(joinPoint, checkAccess);

            assertThat(result).isEqualTo("allowed");
            then(joinPoint).should().proceed();
        }

        @Test
        @DisplayName("null 표현식도 타입 전체 권한으로 평가한다")
        void null_표현식은_타입_전체_권한이다() throws Throwable {
            given(checkAccess.resourceId()).willReturn(null);
            given(checkPermissionUseCase.check(
                MEMBER_ID,
                ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ)
            )).willReturn(true);

            sut.checkAccess(joinPoint, checkAccess);

            then(checkPermissionUseCase).should().check(
                MEMBER_ID,
                ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ)
            );
        }

        @Test
        @DisplayName("메서드 인자 표현식은 문자열 리소스 ID로 변환한다")
        void 메서드_인자를_리소스_id로_변환한다() throws Throwable {
            given(checkAccess.resourceId()).willReturn("#challengerId");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getParameterNames()).willReturn(new String[]{"challengerId", "ignored"});
            given(joinPoint.getArgs()).willReturn(new Object[]{31L, "value"});
            ResourcePermission permission =
                ResourcePermission.of(ResourceType.CHALLENGER_ROLE, "31", PermissionType.READ);
            given(checkPermissionUseCase.check(MEMBER_ID, permission)).willReturn(true);

            sut.checkAccess(joinPoint, checkAccess);

            then(checkPermissionUseCase).should().check(MEMBER_ID, permission);
        }

        @Test
        @DisplayName("표현식 결과가 null이면 타입 전체 권한으로 평가한다")
        void null_평가값은_타입_전체_권한이다() throws Throwable {
            given(checkAccess.resourceId()).willReturn("#challengerId");
            given(joinPoint.getSignature()).willReturn(methodSignature);
            given(methodSignature.getParameterNames()).willReturn(new String[]{"challengerId"});
            given(joinPoint.getArgs()).willReturn(new Object[]{null});
            ResourcePermission permission =
                ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ);
            given(checkPermissionUseCase.check(MEMBER_ID, permission)).willReturn(true);

            sut.checkAccess(joinPoint, checkAccess);

            then(checkPermissionUseCase).should().check(MEMBER_ID, permission);
        }
    }

    @Test
    @DisplayName("권한 평가가 false이면 지정한 메시지의 도메인 예외를 던지고 원 메서드를 실행하지 않는다")
    void 권한이_없으면_원_메서드를_실행하지_않는다() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(MEMBER_ID), null, List.of())
        );
        given(checkAccess.resourceId()).willReturn("");
        given(checkAccess.message()).willReturn("운영진만 접근할 수 있습니다.");
        given(checkPermissionUseCase.check(
            MEMBER_ID,
            ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ)
        )).willReturn(false);

        assertThatThrownBy(() -> sut.checkAccess(joinPoint, checkAccess))
            .isInstanceOfSatisfying(AuthorizationDomainException.class, exception -> {
                assertThat(exception.getBaseCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED);
                assertThat(exception.getMessage()).contains("운영진만 접근할 수 있습니다");
            });
        then(joinPoint).shouldHaveNoInteractions();
    }
}
