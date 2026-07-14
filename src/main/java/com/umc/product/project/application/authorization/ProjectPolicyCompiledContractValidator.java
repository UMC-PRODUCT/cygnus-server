package com.umc.product.project.application.authorization;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.umc.product.authorization.domain.policy.CompiledPolicyBundle;
import com.umc.product.authorization.domain.policy.CompiledPolicyModule;
import com.umc.product.authorization.domain.policy.CompiledPolicyOutcome;
import com.umc.product.authorization.domain.policy.CompiledPolicyStatement;
import com.umc.product.authorization.domain.policy.PolicyOperand;

public class ProjectPolicyCompiledContractValidator {

    private static final String SCHEMA_VERSION = "1.0";
    private static final String POLICY_VERSION = "1.1.0";
    private static final String NAMESPACE = "project";

    private static final Set<String> PROJECT_SELECTORS = Set.of(
        ProjectPolicyOutcomes.PROJECT_ALL,
        ProjectPolicyOutcomes.PROJECT_PUBLIC_ONLY,
        ProjectPolicyOutcomes.PROJECT_GISU_IDS,
        ProjectPolicyOutcomes.PROJECT_CHAPTER_IDS,
        ProjectPolicyOutcomes.PROJECT_OWNER_MEMBER_IDS
    );
    private static final Set<String> APPLICATION_SELECTORS = Set.of(
        ProjectPolicyOutcomes.APPLICATION_ALL,
        ProjectPolicyOutcomes.APPLICATION_GISU_IDS,
        ProjectPolicyOutcomes.APPLICATION_CHAPTER_IDS,
        ProjectPolicyOutcomes.APPLICATION_PROJECT_IDS,
        ProjectPolicyOutcomes.APPLICATION_OWNER_MEMBER_IDS
    );
    private static final Set<String> PROJECT_MODIFIERS = Set.of(
        ProjectPolicyOutcomes.PROJECT_INCLUDE_OWN_DRAFTS
    );
    private static final Set<String> APPLICATION_MODIFIERS = Set.of(
        ProjectPolicyOutcomes.APPLICATION_INCLUDE_ONGOING_ROUNDS
    );
    private static final Set<ProjectPolicyAction> PROJECT_SCOPE_ACTIONS = EnumSet.of(
        ProjectPolicyAction.PROJECT_LIST_PUBLIC,
        ProjectPolicyAction.PROJECT_LIST_MANAGED,
        ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS
    );
    private static final Set<ProjectPolicyAction> APPLICATION_SCOPE_ACTIONS = EnumSet.of(
        ProjectPolicyAction.APPLICATION_LIST_SELF,
        ProjectPolicyAction.APPLICATION_LIST_PROJECT,
        ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH,
        ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT
    );

    public void validate(CompiledPolicyBundle bundle) {
        require(SCHEMA_VERSION.equals(bundle.schemaVersion()), "Project policy schemaVersion이 일치하지 않습니다.");
        require(ProjectPolicyDomainSchema.VERSION.equals(bundle.contextSchemaVersion()),
            "Project policy contextSchemaVersion이 일치하지 않습니다.");
        require(NAMESPACE.equals(bundle.namespace()), "Project policy namespace가 일치하지 않습니다.");
        require(POLICY_VERSION.equals(bundle.policyVersion()), "Project policyVersion이 일치하지 않습니다.");

        Map<ProjectPolicyModule, CompiledPolicyModule> modules = indexModules(bundle);
        EnumSet<ProjectPolicyAction> covered = EnumSet.noneOf(ProjectPolicyAction.class);
        for (Map.Entry<ProjectPolicyModule, CompiledPolicyModule> entry : modules.entrySet()) {
            for (CompiledPolicyStatement statement : entry.getValue().statements()) {
                validateStatement(entry.getKey(), statement, bundle, covered);
            }
        }
        require(covered.equals(EnumSet.allOf(ProjectPolicyAction.class)),
            "Project policy action coverage가 일치하지 않습니다: " + covered);
    }

    private Map<ProjectPolicyModule, CompiledPolicyModule> indexModules(CompiledPolicyBundle bundle) {
        Map<ProjectPolicyModule, CompiledPolicyModule> result = new EnumMap<>(ProjectPolicyModule.class);
        for (CompiledPolicyModule module : bundle.modules()) {
            ProjectPolicyModule expected = java.util.Arrays.stream(ProjectPolicyModule.values())
                .filter(candidate -> candidate.id().equals(module.id()))
                .findFirst()
                .orElseThrow(() -> invalid("선언되지 않은 Project policy module입니다: " + module.id()));
            require(module.filename().equals(expected.id() + ".policy.json"),
                "Project policy module filename이 일치하지 않습니다: " + module.filename());
            require(result.putIfAbsent(expected, module) == null,
                "Project policy module이 중복되었습니다: " + expected.id());
        }
        require(result.keySet().equals(EnumSet.allOf(ProjectPolicyModule.class)),
            "Project policy module manifest가 일치하지 않습니다: " + result.keySet());
        return Map.copyOf(result);
    }

    private void validateStatement(
        ProjectPolicyModule module,
        CompiledPolicyStatement statement,
        CompiledPolicyBundle bundle,
        Set<ProjectPolicyAction> covered
    ) {
        Set<String> outcomeKeys = new HashSet<>();
        statement.outcomes().stream().map(CompiledPolicyOutcome::key).forEach(outcomeKeys::add);
        long projectSelectorCount = outcomeKeys.stream().filter(PROJECT_SELECTORS::contains).count();
        long applicationSelectorCount = outcomeKeys.stream().filter(APPLICATION_SELECTORS::contains).count();
        long projectModifierCount = outcomeKeys.stream().filter(PROJECT_MODIFIERS::contains).count();
        long applicationModifierCount = outcomeKeys.stream().filter(APPLICATION_MODIFIERS::contains).count();
        require(projectSelectorCount <= 1 && applicationSelectorCount <= 1,
            "한 statement는 scope selector 차원을 하나만 방출할 수 있습니다: " + statement.id());
        require(projectSelectorCount == 0 || applicationSelectorCount == 0,
            "Project/Application selector를 한 statement에서 혼합할 수 없습니다: " + statement.id());

        for (String actionId : statement.actions()) {
            ProjectPolicyAction action = ProjectPolicyAction.fromId(actionId);
            require(expectedModule(action) == module,
                "Project policy action module이 일치하지 않습니다: " + actionId);
            validateScopeOutcomes(action, statement.id(), projectSelectorCount, applicationSelectorCount,
                projectModifierCount, applicationModifierCount);
            covered.add(action);
            for (CompiledPolicyOutcome outcome : statement.outcomes()) {
                if (outcome.value() instanceof PolicyOperand.Attribute attribute) {
                    boolean required = bundle.domainSchema().action(actionId).orElseThrow()
                        .requiredAttributes().contains(attribute.name());
                    require(required, "Outcome ATTRIBUTE는 required attribute여야 합니다: " + statement.id());
                }
            }
        }
    }

    private void validateScopeOutcomes(
        ProjectPolicyAction action,
        String statementId,
        long projectSelectorCount,
        long applicationSelectorCount,
        long projectModifierCount,
        long applicationModifierCount
    ) {
        if (PROJECT_SCOPE_ACTIONS.contains(action)) {
            require(projectSelectorCount == 1 && applicationSelectorCount == 0,
                "Project scope statement는 project selector를 정확히 하나 방출해야 합니다: " + statementId);
            require(applicationModifierCount == 0,
                "Project scope statement는 application modifier를 방출할 수 없습니다: " + statementId);
            return;
        }
        if (APPLICATION_SCOPE_ACTIONS.contains(action)) {
            require(applicationSelectorCount == 1 && projectSelectorCount == 0,
                "Application scope statement는 application selector를 정확히 하나 방출해야 합니다: " + statementId);
            require(projectModifierCount == 0,
                "Application scope statement는 project modifier를 방출할 수 없습니다: " + statementId);
            return;
        }
        require(projectSelectorCount == 0 && applicationSelectorCount == 0,
            "Non-scope statement는 selector outcome을 방출할 수 없습니다: " + statementId);
        require(projectModifierCount == 0 && applicationModifierCount == 0,
            "Scope modifier는 selector가 있는 scope statement에서만 사용할 수 있습니다: " + statementId);
    }

    private ProjectPolicyModule expectedModule(ProjectPolicyAction action) {
        if (PROJECT_SCOPE_ACTIONS.contains(action)) {
            return ProjectPolicyModule.PROJECT_SCOPE;
        }
        if (APPLICATION_SCOPE_ACTIONS.contains(action)) {
            return ProjectPolicyModule.APPLICATION_SCOPE;
        }
        if (action.name().startsWith("APPLICATION_")) {
            return ProjectPolicyModule.APPLICATION_RESOURCE;
        }
        if (action.name().startsWith("FORM_")) {
            return ProjectPolicyModule.FORM;
        }
        if (action.name().startsWith("MATCHING_")) {
            return ProjectPolicyModule.MATCHING_ROUND;
        }
        if (action.name().startsWith("STATISTICS_")) {
            return ProjectPolicyModule.STATISTICS;
        }
        return ProjectPolicyModule.PROJECT_RESOURCE;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw invalid(message);
        }
    }

    private IllegalStateException invalid(String message) {
        return new IllegalStateException(message);
    }
}
