package com.umc.product.audit.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record AuditDetails(
    int schemaVersion,
    Actor actor,
    Target target,
    Context context,
    State before,
    State after
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public AuditDetails {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("지원하지 않는 감사 details schemaVersion입니다: " + schemaVersion);
        }
        actor = actor == null ? Actor.empty() : actor;
        target = target == null ? Target.empty() : target;
        context = context == null ? Context.empty() : context;
        before = before == null ? State.empty() : before;
        after = after == null ? State.empty() : after;
    }

    public static AuditDetails of(
        Actor actor,
        Target target,
        Context context,
        State before,
        State after
    ) {
        return new AuditDetails(CURRENT_SCHEMA_VERSION, actor, target, context, before, after);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("schemaVersion", schemaVersion);
        details.put("actor", actor.values());
        details.put("target", target.values());
        details.put("context", context.values());
        details.put("before", before.values());
        details.put("after", after.values());
        return Collections.unmodifiableMap(details);
    }

    public record Actor(Map<String, Object> values) {

        public Actor {
            values = AuditDetailsPolicy.sanitizeSection(values, AuditDetailsPolicy.Section.ACTOR);
        }

        public static Actor from(Map<String, ?> values) {
            return new Actor(copy(values));
        }

        public static Actor empty() {
            return new Actor(Map.of());
        }
    }

    public record Target(Map<String, Object> values) {

        public Target {
            values = AuditDetailsPolicy.sanitizeSection(values, AuditDetailsPolicy.Section.TARGET);
        }

        public static Target from(Map<String, ?> values) {
            return new Target(copy(values));
        }

        public static Target empty() {
            return new Target(Map.of());
        }
    }

    public record Context(Map<String, Object> values) {

        public Context {
            values = AuditDetailsPolicy.sanitizeSection(values, AuditDetailsPolicy.Section.CONTEXT);
        }

        public static Context from(Map<String, ?> values) {
            return new Context(copy(values));
        }

        public static Context empty() {
            return new Context(Map.of());
        }
    }

    public record State(Map<String, Object> values) {

        public State {
            values = AuditDetailsPolicy.sanitizeSection(values, AuditDetailsPolicy.Section.STATE);
        }

        public static State from(Map<String, ?> values) {
            return new State(copy(values));
        }

        public static State empty() {
            return new State(Map.of());
        }
    }

    private static Map<String, Object> copy(Map<String, ?> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return new LinkedHashMap<>(values);
    }
}
