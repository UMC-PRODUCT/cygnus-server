package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationRolloutModeResolverTest {

    @Test
    @DisplayName("action override는 전역 rollout mode보다 우선한다")
    void actionOverrideTakesPrecedenceOverGlobalMode() {
        // Given
        var configuration = new ProjectAuthorizationRolloutConfiguration(
            ProjectAuthorizationRolloutMode.SHADOW,
            Map.of(ProjectPolicyAction.PROJECT_READ, ProjectAuthorizationRolloutMode.ENFORCE)
        );
        var resolver = new ConfiguredProjectAuthorizationRolloutModeResolver(configuration);

        // When
        ProjectAuthorizationRolloutMode overridden = resolver.resolve(ProjectPolicyAction.PROJECT_READ);
        ProjectAuthorizationRolloutMode inherited = resolver.resolve(ProjectPolicyAction.PROJECT_UPDATE);

        // Then
        assertThat(overridden).isEqualTo(ProjectAuthorizationRolloutMode.ENFORCE);
        assertThat(inherited).isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
    }
}
