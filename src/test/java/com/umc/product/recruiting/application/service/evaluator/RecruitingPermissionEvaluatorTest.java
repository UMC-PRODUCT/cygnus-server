package com.umc.product.recruiting.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAction;
import com.umc.product.recruiting.application.authorization.RecruitingPolicyAuthorizationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitingPermissionEvaluator")
class RecruitingPermissionEvaluatorTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long SEASON_ID = 44L;

    @Mock
    RecruitingPolicyAuthorizationService policyAuthorizationService;

    RecruitingPermissionEvaluator sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingPermissionEvaluator(policyAuthorizationService);
    }

    @Test
    @DisplayName("supportedResourceType은 RECRUITMENT를 반환한다")
    void supportedResourceType은_RECRUITMENT를_반환한다() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.RECRUITMENT);
    }

    @Test
    @DisplayName("학교 회장단은 자기 학교 모집 WRITE 권한을 통과한다")
    void 학교_회장단은_자기_학교_모집_WRITE_권한을_통과한다() {
        SubjectAttributes subject = subject();
        given(policyAuthorizationService.evaluateResource(
            RecruitingPolicyAction.OPERATE_SCHOOL,
            subject,
            SEASON_ID)).willReturn(true);

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isTrue();
    }

    @Test
    @DisplayName("학교 회장단은 다른 학교 모집 WRITE 권한을 거부한다")
    void 학교_회장단은_다른_학교_모집_WRITE_권한을_거부한다() {
        SubjectAttributes subject = subject();

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("중앙운영사무국 총괄단은 모든 학교 모집 APPROVE 권한을 통과한다")
    void 중앙운영사무국_총괄단은_모든_학교_모집_APPROVE_권한을_통과한다() {
        SubjectAttributes subject = subject();
        given(policyAuthorizationService.evaluateResource(
            RecruitingPolicyAction.OPERATE_SCHOOL,
            subject,
            SEASON_ID)).willReturn(true);

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.APPROVE))).isTrue();
    }

    @Test
    @DisplayName("중앙운영사무국 총괄단은 모든 학교 모집 MANAGE 권한을 통과한다")
    void 중앙운영사무국_총괄단은_모든_학교_모집_MANAGE_권한을_통과한다() {
        SubjectAttributes subject = subject();
        given(policyAuthorizationService.evaluateResource(
            RecruitingPolicyAction.MANAGE_ALL,
            subject,
            SEASON_ID)).willReturn(true);

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 기수와 무관하게 특정 모집 MANAGE 권한을 통과한다")
    void SUPER_ADMIN은_기수와_무관하게_특정_모집_MANAGE_권한을_통과한다() {
        SubjectAttributes subject = subject();
        given(policyAuthorizationService.evaluateResource(
            RecruitingPolicyAction.MANAGE_ALL,
            subject,
            SEASON_ID)).willReturn(true);

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isTrue();
    }

    @Test
    @DisplayName("SUPER_ADMIN은 모집 전체 MANAGE 권한을 통과한다")
    void SUPER_ADMIN은_모집_전체_MANAGE_권한을_통과한다() {
        SubjectAttributes subject = subject();
        given(policyAuthorizationService.evaluateResource(
            RecruitingPolicyAction.MANAGE_ALL,
            subject,
            null)).willReturn(true);

        assertThat(sut.evaluate(
            subject,
            ResourcePermission.ofType(ResourceType.RECRUITMENT, PermissionType.MANAGE)
        )).isTrue();
    }

    @Test
    @DisplayName("학교 회장단은 자기 학교라도 MANAGE 권한을 거부한다")
    void 학교_회장단은_자기_학교라도_MANAGE_권한을_거부한다() {
        SubjectAttributes subject = subject();

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.MANAGE))).isFalse();
    }

    @Test
    @DisplayName("교내 파트장은 모집 WRITE 권한을 거부한다")
    void 교내_파트장은_모집_WRITE_권한을_거부한다() {
        SubjectAttributes subject = subject();

        assertThat(sut.evaluate(subject, seasonPermission(PermissionType.WRITE))).isFalse();
    }

    @Test
    @DisplayName("DELETE 권한은 evaluator에서 구현하지 않아 예외가 발생한다")
    void DELETE_권한은_evaluator에서_구현하지_않아_예외가_발생한다() {
        SubjectAttributes subject = subject();

        assertThatThrownBy(() -> sut.evaluate(subject, seasonPermission(PermissionType.DELETE)))
            .isInstanceOf(AuthorizationDomainException.class);
    }

    private ResourcePermission seasonPermission(PermissionType permissionType) {
        return ResourcePermission.of(ResourceType.RECRUITMENT, SEASON_ID, permissionType);
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .memberId(MEMBER_ID)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .systemRoles(Set.of())
            .build();
    }
}
