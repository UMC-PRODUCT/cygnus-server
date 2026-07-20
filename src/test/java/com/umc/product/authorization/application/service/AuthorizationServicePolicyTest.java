package com.umc.product.authorization.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.logging.OperationalMetrics;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationService 권한 정책")
class AuthorizationServicePolicyTest {

    private static final Long MEMBER_ID = 7L;
    private static final ResourcePermission PERMISSION =
        ResourcePermission.ofType(ResourceType.CHALLENGER_ROLE, PermissionType.READ);
    private static final SubjectAttributes SUBJECT = SubjectAttributes.builder()
        .memberId(MEMBER_ID)
        .build();

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    ResourcePermissionEvaluator evaluator;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @Mock
    GetChapterUseCase getChapterUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    OperationalMetrics operationalMetrics;

    @Mock
    CacheUseCase cacheUseCase;

    @Mock
    AuthoritySnapshotCacheSerializer serializer;

    AuthorizationService sut;

    @BeforeEach
    void setUp() {
        given(evaluator.supportedResourceType()).willReturn(ResourceType.CHALLENGER_ROLE);
        sut = service(List.of(evaluator));
    }

    @Nested
    @DisplayName("평가기 선택")
    class EvaluatorSelection {

        @Test
        @DisplayName("등록된 리소스 평가기의 결과를 그대로 반환한다")
        void 등록된_평가기의_결과를_반환한다() {
            given(evaluator.evaluate(SUBJECT, PERMISSION)).willReturn(true);

            assertThat(sut.check(SUBJECT, PERMISSION)).isTrue();
            then(evaluator).should().evaluate(SUBJECT, PERMISSION);
        }

        @Test
        @DisplayName("회원 ID 기반 check는 subject를 로드한 뒤 동일 평가기에 위임한다")
        void 회원_ID로_subject를_로드해_평가한다() {
            AuthorizationService spy = spy(sut);
            doReturn(SUBJECT).when(spy).loadSubject(MEMBER_ID);
            given(evaluator.evaluate(SUBJECT, PERMISSION)).willReturn(true);

            assertThat(spy.check(MEMBER_ID, PERMISSION)).isTrue();
        }

        @Test
        @DisplayName("평가기가 거부한 권한은 false를 반환한다")
        void 평가기가_거부하면_false다() {
            given(evaluator.evaluate(SUBJECT, PERMISSION)).willReturn(false);

            assertThat(sut.check(SUBJECT, PERMISSION)).isFalse();
        }

        @Test
        @DisplayName("리소스 타입에 대응하는 평가기가 없으면 명시적인 예외를 던진다")
        void 평가기가_없으면_예외다() {
            AuthorizationService serviceWithoutEvaluator = service(List.of());

            assertThatThrownBy(() -> serviceWithoutEvaluator.check(SUBJECT, PERMISSION))
                .isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                    assertThat(exception.getBaseCode())
                        .isEqualTo(AuthorizationErrorCode.NO_EVALUATOR_MATCHING_RESOURCE_TYPE)
                );
        }

        @Test
        @DisplayName("같은 리소스 타입 평가기가 중복 등록되면 애플리케이션 시작 시 실패한다")
        void 중복_평가기_등록은_실패한다() {
            ResourcePermissionEvaluator duplicate = org.mockito.Mockito.mock(ResourcePermissionEvaluator.class);
            given(duplicate.supportedResourceType()).willReturn(ResourceType.CHALLENGER_ROLE);

            assertThatThrownBy(() -> service(List.of(evaluator, duplicate)))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("거부 예외와 보안 지표")
    class Denial {

        @Test
        @DisplayName("허용된 요청은 예외나 거부 지표 없이 통과한다")
        void 허용된_요청은_통과한다() {
            AuthorizationService spy = org.mockito.Mockito.spy(sut);
            org.mockito.Mockito.doReturn(true).when(spy).check(MEMBER_ID, PERMISSION);

            spy.checkOrThrow(MEMBER_ID, PERMISSION);

            then(operationalMetrics).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("거부된 요청은 보안 지표를 기록하고 접근 거부 예외를 던진다")
        void 거부된_요청은_지표와_예외를_남긴다() {
            AuthorizationService spy = org.mockito.Mockito.spy(sut);
            org.mockito.Mockito.doReturn(false).when(spy).check(MEMBER_ID, PERMISSION);

            assertThatThrownBy(() -> spy.checkOrThrow(MEMBER_ID, PERMISSION))
                .isInstanceOfSatisfying(AuthorizationDomainException.class, exception ->
                    assertThat(exception.getBaseCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED)
                );
            then(operationalMetrics).should().recordSecurityEvent("AUTHORIZATION", "ACCESS_DENIED", "denied");
        }
    }

    private AuthorizationService service(List<ResourcePermissionEvaluator> evaluators) {
        return new AuthorizationService(
            loadChallengerRolePort,
            evaluators,
            getMemberUseCase,
            listMemberSystemRoleUseCase,
            getChapterUseCase,
            getChallengerUseCase,
            operationalMetrics,
            cacheUseCase,
            serializer
        );
    }
}
