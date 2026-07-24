package com.umc.product.schedule.application.service.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.schedule.application.authorization.SchedulePolicyAction;
import com.umc.product.schedule.application.authorization.SchedulePolicyAuthorizationService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SchedulePermissionEvaluator")
class SchedulePermissionEvaluatorTest {

    private static final Long SCHEDULE_ID = 100L;

    @Mock
    SchedulePolicyAuthorizationService policyAuthorizationService;
    @Mock
    SubjectAttributes subjectAttributes;
    @InjectMocks
    SchedulePermissionEvaluator sut;

    @Test
    @DisplayName("지원 resource type은 SCHEDULE이다")
    void supportedResourceType() {
        assertThat(sut.supportedResourceType()).isEqualTo(ResourceType.SCHEDULE);
    }

    @ParameterizedTest(name = "{0} 권한은 {1} action으로 위임한다")
    @MethodSource("permissionActionCases")
    @DisplayName("resource permission을 semantic policy action으로 변환한다")
    void delegateToSemanticPolicyAction(
        PermissionType permissionType,
        SchedulePolicyAction policyAction
    ) {
        ResourcePermission permission = ResourcePermission.of(
            ResourceType.SCHEDULE,
            SCHEDULE_ID,
            permissionType);
        given(policyAuthorizationService.evaluate(
            policyAction,
            subjectAttributes,
            SCHEDULE_ID
        )).willReturn(true);

        boolean result = sut.evaluate(subjectAttributes, permission);

        assertThat(result).isTrue();
        verify(policyAuthorizationService).evaluate(
            policyAction,
            subjectAttributes,
            SCHEDULE_ID);
    }

    private static Stream<Arguments> permissionActionCases() {
        return Stream.of(
            Arguments.of(PermissionType.READ, SchedulePolicyAction.SCHEDULE_READ),
            Arguments.of(PermissionType.WRITE, SchedulePolicyAction.SCHEDULE_CREATE),
            Arguments.of(PermissionType.EDIT, SchedulePolicyAction.SCHEDULE_UPDATE),
            Arguments.of(PermissionType.DELETE, SchedulePolicyAction.SCHEDULE_DELETE),
            Arguments.of(PermissionType.FORCE_DELETE, SchedulePolicyAction.SCHEDULE_FORCE_DELETE)
        );
    }
}
