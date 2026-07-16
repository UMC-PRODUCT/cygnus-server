package com.umc.product.challenger.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerPointPermissionEvaluator")
class ChallengerPointPermissionEvaluatorTest {

    private static final Long CHALLENGER_POINT_ID = 50L;
    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerPointUseCase getChallengerPointUseCase;

    @Test
    @DisplayName("상벌점 삭제 권한은 기존 호환성을 위해 중앙 총괄단의 다른 기수 역할도 인정한다")
    void central_core_in_other_gisu_can_delete_challenger_point_for_compatibility() {
        ChallengerPointPermissionEvaluator sut = new ChallengerPointPermissionEvaluator(
            getChallengerUseCase,
            getMemberUseCase,
            getChallengerPointUseCase
        );
        SubjectAttributes subject = SubjectAttributes.builder()
            .memberId(1L)
            .schoolId(30L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of(new RoleAttribute(
                ChallengerRoleType.CENTRAL_VICE_PRESIDENT,
                OrganizationType.CENTRAL,
                null,
                null,
                9L
            )))
            .systemRoles(Set.of())
            .build();

        boolean result = sut.evaluate(
            subject,
            ResourcePermission.of(ResourceType.CHALLENGER_POINT, CHALLENGER_POINT_ID, PermissionType.DELETE)
        );

        assertThat(result).isTrue();
    }
}
