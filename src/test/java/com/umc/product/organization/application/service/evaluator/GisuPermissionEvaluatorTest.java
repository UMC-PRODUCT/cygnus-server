package com.umc.product.organization.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.organization.application.authorization.OrganizationPolicyAction;
import com.umc.product.organization.application.authorization.OrganizationPolicyAuthorizationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("GisuPermissionEvaluator")
class GisuPermissionEvaluatorTest {

    @Mock
    OrganizationPolicyAuthorizationService policyAuthorizationService;

    @Test
    @DisplayName("기수 관리 권한은 기존 호환성을 위해 중앙 총괄단의 다른 기수 역할도 인정한다")
    void central_core_in_other_gisu_can_manage_gisu_for_compatibility() {
        GisuPermissionEvaluator sut = new GisuPermissionEvaluator(policyAuthorizationService);
        SubjectAttributes subject = SubjectAttributes.builder().memberId(1L).build();
        given(policyAuthorizationService.evaluate(
            OrganizationPolicyAction.GISU_UPDATE,
            subject)).willReturn(true);

        boolean result = sut.evaluate(subject, ResourcePermission.of(ResourceType.GISU, 10L, PermissionType.EDIT));

        assertThat(result).isTrue();
        then(policyAuthorizationService).should()
            .evaluate(OrganizationPolicyAction.GISU_UPDATE, subject);
    }
}
