package com.umc.product.project.application.authorization.rollout;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;

import com.umc.product.authorization.application.port.in.policy.CompilePolicyBundleUseCase;
import com.umc.product.authorization.application.port.in.policy.EvaluatePolicyUseCase;
import com.umc.product.authorization.application.service.policy.PolicyEvaluationService;
import com.umc.product.authorization.application.service.policy.PolicySemanticCompiler;
import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationAdapter;
import com.umc.product.project.application.authorization.rollout.target.TargetProjectAuthorizationAdapter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ProjectAuthorizationRolloutApplicationContextTest {

    @Test
    @DisplayName("rollout 설정이 없으면 SHADOW와 빈 override로 시작하고 구체 구현 bean을 구성한다")
    void 설정이_없으면_shadow와_빈_override로_시작하고_구체_bean을_구성한다() {
        AtomicInteger compileCount = new AtomicInteger();

        contextRunner(compileCount).run(context -> {
            ProjectAuthorizationRolloutConfiguration configuration =
                context.getBean(ProjectAuthorizationRolloutConfiguration.class);

            assertThat(context).hasNotFailed();
            assertThat(configuration.defaultMode()).isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
            assertThat(configuration.actionOverrides()).isEmpty();
            assertThat(context).hasSingleBean(LegacyProjectAuthorizationAdapter.class);
            assertThat(context).hasSingleBean(TargetProjectAuthorizationAdapter.class);
            assertThat(context).hasSingleBean(ProjectAuthorizationClassifier.class);
            assertThat(context).hasSingleBean(ProjectAuthorizationRolloutCoordinator.class);
            assertThat(context).hasSingleBean(ProjectAuthorizationRolloutObserver.class);
            assertThat(context).hasSingleBean(Clock.class);
            assertThat(context.getBean(ProjectAuthorizationRolloutObserver.class))
                .isInstanceOf(ProjectAuthorizationRolloutObserver.MicrometerObserver.class);
            assertThat(compileCount).hasValue(1);
        });
    }

    @Test
    @DisplayName("packaged base 설정은 project authorization rollout을 SHADOW로 고정한다")
    void packaged_base_설정은_project_authorization_rollout을_shadow로_고정한다() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application.yml"));

        Properties properties = factory.getObject();

        assertThat(properties)
            .containsEntry(
                "app.project.authorization-rollout.default-mode",
                "${PROJECT_AUTHORIZATION_ROLLOUT_DEFAULT_MODE:SHADOW}"
            );
    }

    @Test
    @DisplayName("global SHADOW 설정을 모든 override 없는 action에 적용한다")
    void global_shadow를_override_없는_action에_적용한다() {
        contextRunner(new AtomicInteger())
            .withPropertyValues("app.project.authorization-rollout.default-mode=SHADOW")
            .run(context -> {
                ProjectAuthorizationRolloutModeResolver resolver =
                    context.getBean(ProjectAuthorizationRolloutModeResolver.class);

                assertThat(context).hasNotFailed();
                assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_READ))
                    .isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
            });
    }

    @Test
    @DisplayName("SHADOW에서 정확한 non-ENFORCE action override가 global mode보다 우선한다")
    void shadow에서_정확한_non_enforce_action_override가_global_mode보다_우선한다() {
        contextRunner(new AtomicInteger())
            .withPropertyValues(
                "app.project.authorization-rollout.default-mode=SHADOW",
                "app.project.authorization-rollout.action-overrides[0].action=project:read",
                "app.project.authorization-rollout.action-overrides[0].mode=LEGACY"
            )
            .run(context -> {
                ProjectAuthorizationRolloutModeResolver resolver =
                    context.getBean(ProjectAuthorizationRolloutModeResolver.class);

                assertThat(context).hasNotFailed();
                assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_READ))
                    .isEqualTo(ProjectAuthorizationRolloutMode.LEGACY);
                assertThat(resolver.resolve(ProjectPolicyAction.PROJECT_UPDATE))
                    .isEqualTo(ProjectAuthorizationRolloutMode.SHADOW);
            });
    }

    @Test
    @DisplayName("global ENFORCE는 empty shipped receipt로 startup을 fail closed한다")
    void global_enforce는_empty_receipt로_startup을_fail_closed한다() {
        assertStartupFailure("app.project.authorization-rollout.default-mode=ENFORCE");
    }

    @Test
    @DisplayName("action ENFORCE override도 empty shipped receipt로 startup을 fail closed한다")
    void action_enforce_override도_empty_receipt로_startup을_fail_closed한다() {
        assertStartupFailure(
            "app.project.authorization-rollout.default-mode=SHADOW",
            "app.project.authorization-rollout.action-overrides[0].action=project:read",
            "app.project.authorization-rollout.action-overrides[0].mode=ENFORCE"
        );
    }

    @Test
    @DisplayName("등록되지 않은 action override는 startup을 실패시킨다")
    void 등록되지_않은_action_override는_startup을_실패시킨다() {
        assertStartupFailure(
            "app.project.authorization-rollout.action-overrides[0].action=project:unknown",
            "app.project.authorization-rollout.action-overrides[0].mode=SHADOW"
        );
    }

    @Test
    @DisplayName("빈 action override는 startup을 실패시킨다")
    void 빈_action_override는_startup을_실패시킨다() {
        assertStartupFailure(
            "app.project.authorization-rollout.action-overrides[0].action=",
            "app.project.authorization-rollout.action-overrides[0].mode=SHADOW"
        );
    }

    @Test
    @DisplayName("동일한 canonical action override는 startup을 실패시킨다")
    void 동일한_canonical_action_override는_startup을_실패시킨다() {
        assertStartupFailure(
            "app.project.authorization-rollout.action-overrides[0].action=project:read",
            "app.project.authorization-rollout.action-overrides[0].mode=SHADOW",
            "app.project.authorization-rollout.action-overrides[1].action=project:read",
            "app.project.authorization-rollout.action-overrides[1].mode=ENFORCE"
        );
    }

    @Test
    @DisplayName("action override mode가 누락되면 startup을 실패시킨다")
    void action_override_mode가_누락되면_startup을_실패시킨다() {
        assertStartupFailure(
            "app.project.authorization-rollout.action-overrides[0].action=project:read"
        );
    }

    @Test
    @DisplayName("유효하지 않은 rollout mode는 startup을 실패시킨다")
    void 유효하지_않은_rollout_mode는_startup을_실패시킨다() {
        assertStartupFailure("app.project.authorization-rollout.default-mode=INVALID");
    }

    @ParameterizedTest(name = "{0} mode")
    @EnumSource(value = ProjectAuthorizationRolloutMode.class, names = {"LEGACY", "SHADOW"})
    @DisplayName("LEGACY와 SHADOW에서도 target policy를 startup에 한 번 compile한다")
    void legacy와_shadow에서_target_policy를_startup에_compile한다(
        ProjectAuthorizationRolloutMode mode
    ) {
        AtomicInteger compileCount = new AtomicInteger();

        contextRunner(compileCount)
            .withPropertyValues("app.project.authorization-rollout.default-mode=" + mode.name())
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(compileCount).hasValue(1);
            });
    }

    @Test
    @DisplayName("ENFORCE receipt gate 실패 전에도 target policy를 startup에 한 번 compile한다")
    void enforce_receipt_gate_실패_전에도_target_policy를_compile한다() {
        AtomicInteger compileCount = new AtomicInteger();
        AtomicInteger coordinatorCreationCount = new AtomicInteger();

        contextRunner(compileCount, coordinatorCreationCount)
            .withPropertyValues("app.project.authorization-rollout.default-mode=ENFORCE")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(compileCount).hasValue(1);
                assertThat(coordinatorCreationCount).hasValue(0);
            });
    }

    @Test
    @DisplayName("test profile은 전체 project authorization을 ENFORCE로 고정하지 않는다")
    void test_profile은_전체_project_authorization을_enforce로_고정하지_않는다() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-test.yml"));

        Properties properties = factory.getObject();

        assertThat(properties)
            .doesNotContainKey("app.project.authorization-rollout.default-mode");
    }

    @Test
    @DisplayName("dev local test prod profile은 전체 ENFORCE를 고정하지 않는다")
    void 배포_profile은_전체_enforce를_고정하지_않는다() {
        for (String profile : List.of("dev", "local", "test", "prod")) {
            ClassPathResource resource = new ClassPathResource("application-" + profile + ".yml");
            if (!resource.exists()) {
                continue;
            }
            YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
            factory.setResources(resource);
            Properties properties = factory.getObject();
            assertThat(properties.getProperty("app.project.authorization-rollout.default-mode"))
                .isNotEqualTo("ENFORCE");
        }
    }

    private void assertStartupFailure(String... propertyValues) {
        AtomicInteger compileCount = new AtomicInteger();
        AtomicInteger coordinatorCreationCount = new AtomicInteger();
        contextRunner(compileCount, coordinatorCreationCount)
            .withPropertyValues(propertyValues)
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(compileCount).hasValue(1);
                assertThat(coordinatorCreationCount).hasValue(0);
            });
    }

    private ApplicationContextRunner contextRunner(AtomicInteger compileCount) {
        return contextRunner(compileCount, new AtomicInteger());
    }

    private ApplicationContextRunner contextRunner(
        AtomicInteger compileCount,
        AtomicInteger coordinatorCreationCount
    ) {
        return new ApplicationContextRunner()
            .withUserConfiguration(
                ProjectAuthorizationRolloutSpringConfiguration.class,
                ProjectPolicyBundleLoader.class,
                LegacyProjectAuthorizationAdapter.class,
                TargetProjectAuthorizationAdapter.class,
                ProjectAuthorizationRolloutObserver.MicrometerObserver.class
            )
            .withBean(
                CompilePolicyBundleUseCase.class,
                () -> request -> {
                    compileCount.incrementAndGet();
                    return new PolicySemanticCompiler().compile(request);
                }
            )
            .withBean(EvaluatePolicyUseCase.class, PolicyEvaluationService::new)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
            .withBean("projectAuthorizationCoordinatorCreationProbe", BeanPostProcessor.class, () ->
                new BeanPostProcessor() {
                    @Override
                    public Object postProcessAfterInitialization(Object bean, String beanName) {
                        if (bean instanceof ProjectAuthorizationRolloutCoordinator) {
                            coordinatorCreationCount.incrementAndGet();
                        }
                        return bean;
                    }
                });
    }
}
