package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.project.application.authorization.ProjectPolicyAction;

class ProjectAuthorizationRolloutSpringConfigurationTest {

    private final ProjectAuthorizationRolloutSpringConfiguration springConfiguration =
        new ProjectAuthorizationRolloutSpringConfiguration();

    @Test
    @DisplayName("rollout 설정이 없으면 모든 action에 SHADOW mode를 적용한다")
    void rollout_설정이_없으면_모든_action에_shadow_mode를_적용한다() {
        ProjectAuthorizationRolloutProperties properties =
            new ProjectAuthorizationRolloutProperties(null, null);

        ProjectAuthorizationRolloutConfiguration configuration =
            springConfiguration.parseConfiguration(properties);
        ProjectAuthorizationRolloutModeResolver resolver =
            springConfiguration.projectAuthorizationRolloutModeResolver(configuration);

        assertThat(properties.defaultMode()).isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
        assertThat(properties.actionOverrides()).isEmpty();
        assertThat(configuration.actionOverrides()).isEmpty();
        assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_READ))
            .isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
    }

    @Test
    @DisplayName("정확한 action ID override는 기본 mode보다 우선한다")
    void 정확한_action_id_override는_기본_mode보다_우선한다() {
        ProjectAuthorizationRolloutProperties properties = new ProjectAuthorizationRolloutProperties(
            ProjectAuthorizationRolloutMode.SHADOW,
            List.of(new ProjectAuthorizationRolloutProperties.ActionOverride(
                ProjectPolicyAction.PROJECT_READ.id(),
                ProjectAuthorizationRolloutMode.ENFORCE
            ))
        );

        ProjectAuthorizationRolloutConfiguration configuration =
            springConfiguration.parseConfiguration(properties);
        ProjectAuthorizationRolloutModeResolver resolver =
            springConfiguration.projectAuthorizationRolloutModeResolver(configuration);

        assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_READ))
            .isEqualTo(ProjectAuthorizationRolloutMode.ENFORCE);
        assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_UPDATE))
            .isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
    }

    @Test
    @DisplayName("등록되지 않은 action ID는 설정 생성을 거부한다")
    void 등록되지_않은_action_id는_설정_생성을_거부한다() {
        ProjectAuthorizationRolloutProperties properties = properties(
            new ProjectAuthorizationRolloutProperties.ActionOverride(
                "project:unknown",
                ProjectAuthorizationRolloutMode.SHADOW
            )
        );

        assertThatThrownBy(() -> springConfiguration.parseConfiguration(properties))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("동일한 action ID override가 중복되면 설정 생성을 거부한다")
    void 동일한_action_id_override가_중복되면_설정_생성을_거부한다() {
        ProjectAuthorizationRolloutProperties.ActionOverride override =
            new ProjectAuthorizationRolloutProperties.ActionOverride(
                ProjectPolicyAction.PROJECT_READ.id(),
                ProjectAuthorizationRolloutMode.SHADOW
            );
        ProjectAuthorizationRolloutProperties properties = new ProjectAuthorizationRolloutProperties(
            ProjectAuthorizationRolloutMode.LEGACY,
            List.of(override, override)
        );

        assertThatThrownBy(() -> springConfiguration.parseConfiguration(properties))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("action override mode가 null이면 설정 생성을 거부한다")
    void action_override_mode가_null이면_설정_생성을_거부한다() {
        ProjectAuthorizationRolloutProperties properties = properties(
            new ProjectAuthorizationRolloutProperties.ActionOverride(
                ProjectPolicyAction.PROJECT_READ.id(),
                null
            )
        );

        assertThatThrownBy(() -> springConfiguration.parseConfiguration(properties))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private ProjectAuthorizationRolloutProperties properties(
        ProjectAuthorizationRolloutProperties.ActionOverride override
    ) {
        return new ProjectAuthorizationRolloutProperties(
            ProjectAuthorizationRolloutMode.LEGACY,
            List.of(override)
        );
    }
}
