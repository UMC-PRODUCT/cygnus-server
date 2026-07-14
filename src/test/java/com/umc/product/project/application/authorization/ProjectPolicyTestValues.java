package com.umc.product.project.application.authorization;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import com.umc.product.authorization.domain.policy.AttributeSchema;
import com.umc.product.authorization.domain.policy.PolicyValue;

final class ProjectPolicyTestValues {

    private static final Instant BASE_INSTANT = Instant.parse("2026-07-13T00:00:00Z");

    private ProjectPolicyTestValues() {}

    static PolicyValue defaultValue(AttributeSchema registered) {
        return switch (registered.type()) {
            case BOOLEAN -> new PolicyValue.BooleanValue(false);
            case LONG -> new PolicyValue.LongValue(1L);
            case STRING -> new PolicyValue.StringValue("default");
            case ENUM -> new PolicyValue.EnumValue(registered.enumSymbols().stream().sorted().findFirst().orElseThrow());
            case INSTANT -> new PolicyValue.InstantValue(BASE_INSTANT);
            case LONG_SET -> new PolicyValue.LongSetValue(Set.of(1L));
            case STRING_SET -> new PolicyValue.StringSetValue(Set.of("default"));
            case ENUM_SET -> new PolicyValue.EnumSetValue(Set.of(
                registered.enumSymbols().stream().sorted().findFirst().orElseThrow()));
            case INSTANT_SET -> new PolicyValue.InstantSetValue(Set.of(BASE_INSTANT));
        };
    }

    static Optional<PolicyValue> differentValue(AttributeSchema registered, PolicyValue value) {
        if (value instanceof PolicyValue.BooleanValue booleanValue) {
            return Optional.of(new PolicyValue.BooleanValue(!booleanValue.value()));
        }
        if (value instanceof PolicyValue.LongValue longValue) {
            long different = longValue.value() == Long.MAX_VALUE ? longValue.value() - 1 : longValue.value() + 1;
            return Optional.of(new PolicyValue.LongValue(different));
        }
        if (value instanceof PolicyValue.StringValue stringValue) {
            return Optional.of(new PolicyValue.StringValue(stringValue.value() + "-different"));
        }
        if (value instanceof PolicyValue.EnumValue enumValue) {
            return registered.enumSymbols().stream().sorted()
                .filter(symbol -> !symbol.equals(enumValue.value()))
                .findFirst()
                .map(PolicyValue.EnumValue::new);
        }
        if (value instanceof PolicyValue.InstantValue instantValue) {
            return Optional.of(new PolicyValue.InstantValue(instantValue.value().plusSeconds(1)));
        }
        return Optional.empty();
    }

    static Optional<PolicyValue> firstElement(PolicyValue set) {
        if (set instanceof PolicyValue.LongSetValue values) {
            return values.value().stream().findFirst().map(PolicyValue.LongValue::new);
        }
        if (set instanceof PolicyValue.StringSetValue values) {
            return values.value().stream().findFirst().map(PolicyValue.StringValue::new);
        }
        if (set instanceof PolicyValue.EnumSetValue values) {
            return values.value().stream().findFirst().map(PolicyValue.EnumValue::new);
        }
        if (set instanceof PolicyValue.InstantSetValue values) {
            return values.value().stream().findFirst().map(PolicyValue.InstantValue::new);
        }
        return Optional.empty();
    }

    static Optional<PolicyValue> outsideElement(AttributeSchema registered, PolicyValue set) {
        if (set instanceof PolicyValue.LongSetValue values) {
            long candidate = 1L;
            while (values.value().contains(candidate)) {
                candidate++;
            }
            return Optional.of(new PolicyValue.LongValue(candidate));
        }
        if (set instanceof PolicyValue.StringSetValue values) {
            String candidate = "outside";
            while (values.value().contains(candidate)) {
                candidate += "-next";
            }
            return Optional.of(new PolicyValue.StringValue(candidate));
        }
        if (set instanceof PolicyValue.EnumSetValue values) {
            return registered.enumSymbols().stream().sorted()
                .filter(symbol -> !values.value().contains(symbol))
                .findFirst()
                .map(PolicyValue.EnumValue::new);
        }
        if (set instanceof PolicyValue.InstantSetValue values) {
            Instant candidate = BASE_INSTANT;
            while (values.value().contains(candidate)) {
                candidate = candidate.plusSeconds(1);
            }
            return Optional.of(new PolicyValue.InstantValue(candidate));
        }
        return Optional.empty();
    }
}
