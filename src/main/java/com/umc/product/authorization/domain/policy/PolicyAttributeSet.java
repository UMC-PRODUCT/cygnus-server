package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public final class PolicyAttributeSet {

    private final NavigableMap<String, PolicyAttribute> attributes;

    private PolicyAttributeSet(NavigableMap<String, PolicyAttribute> attributes) {
        this.attributes = java.util.Collections.unmodifiableNavigableMap(new TreeMap<>(attributes));
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean contains(String name) {
        return attributes.containsKey(name);
    }

    public Optional<PolicyValue> value(String name) {
        PolicyAttribute attribute = attributes.get(name);
        return attribute == null ? Optional.empty() : Optional.of(attribute.value());
    }

    public List<PolicyAttribute> entries() {
        return List.copyOf(attributes.values());
    }

    public record PolicyAttribute(String name, PolicyValueType declaredType, PolicyValue value) {
        public PolicyAttribute {
            Objects.requireNonNull(name);
            Objects.requireNonNull(declaredType);
            Objects.requireNonNull(value);
        }
    }

    public static final class Builder {

        private final NavigableMap<String, PolicyAttribute> attributes = new TreeMap<>();

        private Builder() {}

        public <V extends PolicyValue> Builder put(PolicyAttributeKey<V> key, V value) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(value);
            if (key.type() != value.type()) {
                throw new IllegalArgumentException("Policy attribute key and value types must match");
            }
            PolicyAttribute attribute = new PolicyAttribute(key.name(), key.type(), value);
            if (attributes.putIfAbsent(key.name(), attribute) != null) {
                throw new IllegalArgumentException("Duplicate policy attribute");
            }
            return this;
        }

        public PolicyAttributeSet build() {
            return new PolicyAttributeSet(new TreeMap<>(attributes));
        }
    }
}
