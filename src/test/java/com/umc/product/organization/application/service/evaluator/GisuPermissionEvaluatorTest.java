package com.umc.product.organization.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;

@DisplayName("GisuPermissionEvaluator")
class GisuPermissionEvaluatorTest {

    @Test
    @DisplayName("기수 관리 권한은 기존 호환성을 위해 중앙 총괄단의 다른 기수 역할도 인정한다")
    void central_core_in_other_gisu_can_manage_gisu_for_compatibility() {
        GisuPermissionEvaluator sut = new GisuPermissionEvaluator();
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

        boolean result = sut.evaluate(subject, ResourcePermission.of(ResourceType.GISU, 10L, PermissionType.EDIT));

        assertThat(result).isTrue();
    }
}
