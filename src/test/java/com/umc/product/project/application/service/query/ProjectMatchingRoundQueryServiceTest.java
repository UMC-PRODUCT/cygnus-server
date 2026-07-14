package com.umc.product.project.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.domain.policy.PolicyDecision;
import com.umc.product.authorization.domain.policy.PolicyEffect;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyAuthorizationService;
import com.umc.product.project.application.authorization.ProjectPolicyResourceContext;
import com.umc.product.project.application.port.out.LoadProjectMatchingRoundPort;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

@ExtendWith(MockitoExtension.class)
class ProjectMatchingRoundQueryServiceTest {

    @Mock
    LoadProjectMatchingRoundPort loadProjectMatchingRoundPort;
    @Mock
    ProjectPolicyAuthorizationService projectPolicyAuthorizationService;
    @Mock
    PolicyDecision policyDecision;

    ProjectMatchingRoundQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new ProjectMatchingRoundQueryService(
            loadProjectMatchingRoundPort,
            projectPolicyAuthorizationService
        );
    }

    @Test
    @DisplayName("인증 회원이 matching list 정책을 통과하면 목록을 조회한다")
    void listsRoundsAfterExactMemberPolicyAllows() {
        given(policyDecision.effect()).willReturn(PolicyEffect.ALLOW);
        given(projectPolicyAuthorizationService.evaluate(
            eq(42L),
            eq(ProjectPolicyAction.MATCHING_LIST),
            argThat(ProjectMatchingRoundQueryServiceTest::isEmptyResource)
        )).willReturn(policyDecision);
        given(loadProjectMatchingRoundPort.listByFilters(5L, 10L, null)).willReturn(List.of());

        assertThat(sut.list(42L, 5L, 10L, null)).isEmpty();

        then(loadProjectMatchingRoundPort).should().listByFilters(5L, 10L, null);
    }

    @Test
    @DisplayName("matching list 정책이 거부하면 persistence 조회 전에 차단한다")
    void deniesBeforeLoadingRounds() {
        given(policyDecision.effect()).willReturn(PolicyEffect.DENY);
        given(projectPolicyAuthorizationService.evaluate(
            eq(42L),
            eq(ProjectPolicyAction.MATCHING_LIST),
            argThat(ProjectMatchingRoundQueryServiceTest::isEmptyResource)
        )).willReturn(policyDecision);

        assertThatThrownBy(() -> sut.list(42L, null, null, null))
            .isInstanceOf(ProjectDomainException.class)
            .extracting("baseCode")
            .isEqualTo(ProjectErrorCode.PROJECT_MATCHING_ROUND_ACCESS_DENIED);
        then(loadProjectMatchingRoundPort).should(never()).listByFilters(null, null, null);
    }

    private static boolean isEmptyResource(ProjectPolicyResourceContext resource) {
        return resource != null
            && resource.projectId().isEmpty()
            && resource.gisuId().isEmpty()
            && resource.chapterId().isEmpty()
            && resource.matchingRoundId().isEmpty();
    }
}
