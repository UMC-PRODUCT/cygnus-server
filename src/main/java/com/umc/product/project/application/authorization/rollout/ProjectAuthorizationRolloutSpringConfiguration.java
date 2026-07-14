package com.umc.product.project.application.authorization.rollout;

import java.time.Clock;
import java.util.EnumMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.umc.product.project.application.authorization.ProjectPolicyAction;
import com.umc.product.project.application.authorization.ProjectPolicyBundleLoader;
import com.umc.product.project.application.authorization.rollout.legacy.LegacyProjectAuthorizationAdapter;
import com.umc.product.project.application.authorization.rollout.target.TargetProjectAuthorizationAdapter;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProjectAuthorizationRolloutProperties.class)
public class ProjectAuthorizationRolloutSpringConfiguration {

    @Bean
    public ProjectAuthorizationRolloutConfiguration projectAuthorizationRolloutConfiguration(
        ProjectAuthorizationEnforcementReceiptGate enforcementReceiptGate,
        ProjectAuthorizationRolloutProperties properties
    ) {
        ProjectAuthorizationRolloutConfiguration configuration = parseConfiguration(properties);
        enforcementReceiptGate.validate(configuration);
        return configuration;
    }

    ProjectAuthorizationRolloutConfiguration parseConfiguration(
        ProjectAuthorizationRolloutProperties properties
    ) {
        EnumMap<ProjectPolicyAction, ProjectAuthorizationRolloutMode> overrides =
            new EnumMap<>(ProjectPolicyAction.class);
        for (ProjectAuthorizationRolloutProperties.ActionOverride override : properties.actionOverrides()) {
            if (override.action() == null || override.action().isBlank()) {
                throw new IllegalArgumentException("Project authorization rollout action은 비어 있을 수 없습니다.");
            }
            if (override.mode() == null) {
                throw new IllegalArgumentException("Project authorization rollout mode는 null일 수 없습니다.");
            }
            ProjectPolicyAction action = ProjectPolicyAction.fromId(override.action());
            if (overrides.put(action, override.mode()) != null) {
                throw new IllegalArgumentException("중복된 Project authorization rollout action입니다: " + override.action());
            }
        }
        return new ProjectAuthorizationRolloutConfiguration(properties.defaultMode(), overrides);
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock projectAuthorizationRolloutClock() {
        return Clock.systemUTC();
    }

    @Bean
    public ProjectAuthorizationEnforcementReceiptLoader projectAuthorizationEnforcementReceiptLoader() {
        return new ProjectAuthorizationEnforcementReceiptLoader();
    }

    @Bean
    public ProjectAuthorizationEnforcementReceiptGate projectAuthorizationEnforcementReceiptGate(
        ProjectPolicyBundleLoader policyBundleLoader,
        ProjectAuthorizationEnforcementReceiptLoader receiptLoader,
        Clock clock
    ) {
        LoadedProjectAuthorizationEnforcementReceipts loaded = receiptLoader.load();
        var compiled = policyBundleLoader.compiled().value();
        return new ProjectAuthorizationEnforcementReceiptGate(
            loaded.document(),
            new ProjectAuthorizationEnforcementTarget(
                compiled.policyVersion(),
                compiled.policyFingerprint(),
                loaded.artifactSha256()
            ),
            clock
        );
    }

    @Bean
    public ConfiguredProjectAuthorizationRolloutModeResolver projectAuthorizationRolloutModeResolver(
        ProjectAuthorizationRolloutConfiguration configuration
    ) {
        return new ConfiguredProjectAuthorizationRolloutModeResolver(configuration);
    }

    @Bean
    public ProjectExpectedDifferenceMatrix projectExpectedDifferenceMatrix() {
        return InitialProjectExpectedDifferenceMatrix.create();
    }

    @Bean
    public ProjectAuthorizationClassifier projectAuthorizationClassifier(
        ProjectExpectedDifferenceMatrix expectedDifferenceMatrix
    ) {
        return new ProjectAuthorizationClassifier(expectedDifferenceMatrix);
    }

    @Bean
    public ProjectAuthorizationRolloutCoordinator projectAuthorizationRolloutCoordinator(
        LegacyProjectAuthorizationAdapter legacyEvaluator,
        TargetProjectAuthorizationAdapter targetEvaluator,
        ProjectAuthorizationClassifier classifier,
        ProjectAuthorizationRolloutModeResolver modeResolver
    ) {
        return new ProjectAuthorizationRolloutCoordinator(
            legacyEvaluator,
            targetEvaluator,
            classifier,
            modeResolver
        );
    }
}
