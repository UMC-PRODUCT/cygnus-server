package com.umc.product.authorization.domain.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class PolicyDomainSchema {

    private final String contextSchemaVersion;
    private final Map<String, ActionSchema> actions;
    private final Map<String, AttributeSchema> attributes;
    private final Map<String, OutcomeSchema> outcomes;

    public PolicyDomainSchema(
            String contextSchemaVersion,
            List<ActionSchema> actions,
            List<AttributeSchema> attributes,
            List<OutcomeSchema> outcomes) {
        this.contextSchemaVersion = Objects.requireNonNull(contextSchemaVersion);
        this.actions = index(actions, ActionSchema::actionId);
        this.attributes = index(attributes, AttributeSchema::attributeName);
        this.outcomes = index(outcomes, OutcomeSchema::outcomeKey);
        validateActionContracts();
    }

    public String contextSchemaVersion() {
        return contextSchemaVersion;
    }

    public Optional<ActionSchema> action(String name) {
        return Optional.ofNullable(actions.get(name));
    }

    public Optional<AttributeSchema> attribute(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    public Optional<OutcomeSchema> outcome(String name) {
        return Optional.ofNullable(outcomes.get(name));
    }

    public List<ActionSchema> actions() {
        return actions.values().stream()
            .sorted(java.util.Comparator.comparing(ActionSchema::actionId))
            .toList();
    }

    private void validateActionContracts() {
        for (ActionSchema action : actions.values()) {
            for (String attribute : action.allowedAttributes()) {
                if (!attributes.containsKey(attribute)) {
                    throw new IllegalArgumentException("Action references an unknown attribute");
                }
            }
            for (String outcome : action.allowedOutcomes()) {
                if (!outcomes.containsKey(outcome)) {
                    throw new IllegalArgumentException("Action references an unknown outcome");
                }
            }
        }
    }

    private static <T> Map<String, T> index(List<T> values, Function<T, String> nameExtractor) {
        Objects.requireNonNull(values);
        Map<String, T> indexed = new LinkedHashMap<>();
        for (T value : values) {
            String name = nameExtractor.apply(Objects.requireNonNull(value));
            if (indexed.putIfAbsent(name, value) != null) {
                throw new IllegalArgumentException("Duplicate policy schema name");
            }
        }
        return Map.copyOf(indexed);
    }
}
