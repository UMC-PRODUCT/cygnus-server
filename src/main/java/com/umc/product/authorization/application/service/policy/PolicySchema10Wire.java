package com.umc.product.authorization.application.service.policy;

import java.util.List;
import java.util.Optional;

final class PolicySchema10Wire {

    private PolicySchema10Wire() {}

    record CommonEnvelope(
            String schema,
            String schemaVersion,
            String contextSchemaVersion,
            String namespace,
            String policyVersion) {}

    record Bundle(
            CommonEnvelope envelope,
            String defaultEffect,
            String combiningAlgorithm,
            List<ModuleReference> modules) {}

    record ModuleReference(String id, String resource) {}

    record Module(CommonEnvelope envelope, String moduleId, List<Statement> statements) {}

    record Statement(
            String id, List<String> actions, String effect, Condition condition, List<Outcome> outcomes) {}

    sealed interface Condition permits GroupCondition, PredicateCondition {}

    record GroupCondition(String type, List<Condition> conditions) implements Condition {}

    record PredicateCondition(String operator, Operand left, Optional<Operand> right) implements Condition {}

    record Operand(String type, String name, Object value) {}

    record Outcome(String key, Operand value) {}
}
