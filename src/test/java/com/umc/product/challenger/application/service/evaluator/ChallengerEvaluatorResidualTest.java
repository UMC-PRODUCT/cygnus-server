package com.umc.product.challenger.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.common.domain.exception.CommonException;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;

@DisplayName("Challenger evaluator 잔여 권한 matrix")
class ChallengerEvaluatorResidualTest {

    @Test
    @DisplayName("challenger 생성은 학교 회장단, 수정·삭제는 중앙 총괄단만 허용한다")
    void challenger_권한_matrix() {
        ChallengerPermissionEvaluator sut = new ChallengerPermissionEvaluator();
        SubjectAttributes schoolCore = subject(role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 10L, 9L));
        SubjectAttributes centralCore = subject(role(
            ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null, 9L));
        SubjectAttributes none = subject();

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.CHALLENGER);
        assertThat(sut.evaluate(schoolCore, permission(ResourceType.CHALLENGER, PermissionType.WRITE))).isTrue();
        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER, PermissionType.WRITE))).isFalse();
        assertThat(sut.evaluate(centralCore, permission(ResourceType.CHALLENGER, PermissionType.EDIT))).isTrue();
        assertThat(sut.evaluate(centralCore, permission(ResourceType.CHALLENGER, PermissionType.DELETE))).isTrue();
        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER, PermissionType.DELETE))).isFalse();
        assertUnsupported(sut, none);
    }

    @Test
    @DisplayName("challenger record 조회는 학교 회장단, 생성·삭제는 중앙 총괄단만 허용한다")
    void challenger_record_권한_matrix() {
        ChallengerRecordPermissionController sut = new ChallengerRecordPermissionController();
        SubjectAttributes schoolCore = subject(role(
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 10L, 9L));
        SubjectAttributes centralCore = subject(role(
            ChallengerRoleType.CENTRAL_VICE_PRESIDENT, OrganizationType.CENTRAL, null, 9L));
        SubjectAttributes none = subject();

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.CHALLENGER_RECORD);
        assertThat(sut.evaluate(schoolCore, permission(ResourceType.CHALLENGER_RECORD, PermissionType.READ)))
            .isTrue();
        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER_RECORD, PermissionType.READ))).isFalse();
        assertThat(sut.evaluate(centralCore, permission(ResourceType.CHALLENGER_RECORD, PermissionType.WRITE)))
            .isTrue();
        assertThat(sut.evaluate(centralCore, permission(ResourceType.CHALLENGER_RECORD, PermissionType.DELETE)))
            .isTrue();
        assertUnsupported(sut, none);
    }

    @Test
    @DisplayName("상벌점 생성·수정은 대상 기수 중앙국원 또는 같은 학교 회장단에 허용한다")
    void challenger_point_생성_수정_matrix() {
        GetChallengerUseCase challengerUseCase = mock(GetChallengerUseCase.class);
        GetMemberUseCase memberUseCase = mock(GetMemberUseCase.class);
        GetChallengerPointUseCase pointUseCase = mock(GetChallengerPointUseCase.class);
        ChallengerPointPermissionEvaluator sut =
            new ChallengerPointPermissionEvaluator(challengerUseCase, memberUseCase, pointUseCase);
        given(challengerUseCase.getById(1L)).willReturn(ChallengerInfo.builder()
            .challengerId(1L).memberId(20L).gisuId(9L).build());
        given(memberUseCase.getById(20L)).willReturn(MemberInfo.builder().id(20L).schoolId(10L).build());
        given(pointUseCase.getById(100L)).willReturn(ChallengerPointInfo.builder()
            .id(100L).challengerId(1L).build());
        SubjectAttributes centralMember = subject(role(
            ChallengerRoleType.CENTRAL_OPERATING_TEAM_MEMBER, OrganizationType.CENTRAL, null, 9L));
        SubjectAttributes schoolCore = subject(role(
            ChallengerRoleType.SCHOOL_PRESIDENT, OrganizationType.SCHOOL, 10L, 9L));
        SubjectAttributes none = subject();

        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.CHALLENGER_POINT);
        assertThat(sut.evaluate(centralMember, permission(ResourceType.CHALLENGER_POINT, 1L, PermissionType.WRITE)))
            .isTrue();
        assertThat(sut.evaluate(schoolCore, permission(ResourceType.CHALLENGER_POINT, 1L, PermissionType.WRITE)))
            .isTrue();
        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER_POINT, 1L, PermissionType.WRITE)))
            .isFalse();
        assertThat(sut.evaluate(centralMember, permission(ResourceType.CHALLENGER_POINT, 100L, PermissionType.EDIT)))
            .isTrue();
        assertThat(sut.evaluate(schoolCore, permission(ResourceType.CHALLENGER_POINT, 100L, PermissionType.EDIT)))
            .isTrue();
    }

    @Test
    @DisplayName("대상 학교가 없으면 상벌점 생성·수정을 거부하고 정의되지 않은 권한도 거부한다")
    void challenger_point_null_school과_기본_거부() {
        GetChallengerUseCase challengerUseCase = mock(GetChallengerUseCase.class);
        GetMemberUseCase memberUseCase = mock(GetMemberUseCase.class);
        GetChallengerPointUseCase pointUseCase = mock(GetChallengerPointUseCase.class);
        ChallengerPointPermissionEvaluator sut =
            new ChallengerPointPermissionEvaluator(challengerUseCase, memberUseCase, pointUseCase);
        given(challengerUseCase.getById(1L)).willReturn(ChallengerInfo.builder()
            .challengerId(1L).memberId(20L).gisuId(9L).build());
        given(memberUseCase.getById(20L)).willReturn(MemberInfo.builder().id(20L).schoolId(null).build());
        given(pointUseCase.getById(100L)).willReturn(ChallengerPointInfo.builder()
            .id(100L).challengerId(1L).build());
        SubjectAttributes none = subject();

        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER_POINT, 1L, PermissionType.WRITE)))
            .isFalse();
        assertThat(sut.evaluate(none, permission(ResourceType.CHALLENGER_POINT, 100L, PermissionType.EDIT)))
            .isFalse();
        assertThatThrownBy(() -> sut.evaluate(none, unsupported(PermissionType.READ)))
            .isInstanceOf(CommonException.class);
    }

    private void assertUnsupported(Object evaluator, SubjectAttributes subject) {
        ResourcePermission unsupported = unsupported(
            evaluator instanceof ChallengerRecordPermissionController ? PermissionType.EDIT : PermissionType.READ);
        assertThatThrownBy(() -> {
            if (evaluator instanceof ChallengerPermissionEvaluator challenger) {
                challenger.evaluate(subject, unsupported);
            } else if (evaluator instanceof ChallengerRecordPermissionController record) {
                record.evaluate(subject, unsupported);
            }
        }).isInstanceOf(CommonException.class);
    }

    private ResourcePermission unsupported(PermissionType type) {
        ResourcePermission permission = mock(ResourcePermission.class);
        given(permission.permission()).willReturn(type);
        return permission;
    }

    private ResourcePermission permission(ResourceType type, PermissionType permission) {
        return ResourcePermission.ofType(type, permission);
    }

    private ResourcePermission permission(ResourceType type, Long id, PermissionType permission) {
        return ResourcePermission.of(type, id, permission);
    }

    private SubjectAttributes subject(RoleAttribute... roles) {
        return SubjectAttributes.builder()
            .memberId(1L)
            .roleAttributes(List.of(roles))
            .build();
    }

    private RoleAttribute role(
        ChallengerRoleType roleType,
        OrganizationType organizationType,
        Long organizationId,
        Long gisuId
    ) {
        return new RoleAttribute(roleType, organizationType, organizationId, null, gisuId);
    }
}
