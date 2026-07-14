package com.umc.product.project.application.authorization;

import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_MANAGEMENT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_PROJECT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_PROJECT_BATCH;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.APPLICATION_LIST_SELF;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.CAPABILITY_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.FORM_READ;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_DELETE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_HUMAN_AUTO_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_LIST;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_SYSTEM_AUTO_DECIDE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.MATCHING_UPDATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_CREATE;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_MANAGED;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_OWN_DRAFTS;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.PROJECT_LIST_PUBLIC;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_CHAPTER;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_PROJECT;
import static com.umc.product.project.application.authorization.ProjectPolicyAction.STATISTICS_PUBLIC_MATCHING;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class ProjectDirectActionBindings {

    private static final List<Binding> VALUES = List.of(
        binding(PROJECT_CREATE, Caller.PROJECT_COMMAND),
        binding(PROJECT_LIST_PUBLIC, Caller.PROJECT_SCOPE),
        binding(PROJECT_LIST_MANAGED, Caller.PROJECT_SCOPE),
        binding(PROJECT_LIST_OWN_DRAFTS, Caller.PROJECT_SCOPE),
        binding(APPLICATION_LIST_SELF, Caller.APPLICATION_SCOPE),
        binding(APPLICATION_LIST_PROJECT_BATCH, Caller.APPLICATION_SCOPE),
        binding(APPLICATION_LIST_PROJECT, Caller.APPLICATION_SCOPE),
        binding(APPLICATION_LIST_MANAGEMENT, Caller.APPLICATION_SCOPE),
        binding(FORM_READ, Caller.FORM_QUERY),
        binding(MATCHING_LIST, Caller.MATCHING_QUERY),
        binding(MATCHING_CREATE, Caller.MATCHING_COMMAND),
        binding(MATCHING_UPDATE, Caller.MATCHING_COMMAND),
        binding(MATCHING_DELETE, Caller.MATCHING_COMMAND),
        binding(MATCHING_HUMAN_AUTO_DECIDE, Caller.MATCHING_FINALIZATION),
        binding(MATCHING_SYSTEM_AUTO_DECIDE, Caller.MATCHING_FINALIZATION),
        binding(STATISTICS_PROJECT, Caller.STATISTICS),
        binding(STATISTICS_CHAPTER, Caller.STATISTICS),
        binding(STATISTICS_PUBLIC_MATCHING, Caller.STATISTICS),
        binding(CAPABILITY_LIST, Caller.PERMISSION_QUERY)
    );

    private ProjectDirectActionBindings() {
    }

    static List<Binding> values() {
        return VALUES;
    }

    static List<Binding> valuesFor(Caller first, Caller... rest) {
        EnumSet<Caller> callers = EnumSet.of(first, rest);
        return VALUES.stream().filter(binding -> callers.contains(binding.caller())).toList();
    }

    static Set<ProjectPolicyAction> actions() {
        return VALUES.stream().map(Binding::action).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Binding binding(ProjectPolicyAction action, Caller caller) {
        return new Binding(action, caller);
    }

    enum Caller {
        PROJECT_COMMAND,
        PROJECT_SCOPE,
        APPLICATION_SCOPE,
        FORM_QUERY,
        MATCHING_QUERY,
        MATCHING_COMMAND,
        MATCHING_FINALIZATION,
        STATISTICS,
        PERMISSION_QUERY
    }

    record Binding(ProjectPolicyAction action, Caller caller) {
        @Override
        public String toString() {
            return action.id();
        }
    }
}
