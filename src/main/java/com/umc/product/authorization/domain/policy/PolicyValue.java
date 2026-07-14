package com.umc.product.authorization.domain.policy;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public sealed interface PolicyValue
        permits PolicyValue.BooleanValue,
                PolicyValue.LongValue,
                PolicyValue.StringValue,
                PolicyValue.EnumValue,
                PolicyValue.InstantValue,
                PolicyValue.LongSetValue,
                PolicyValue.StringSetValue,
                PolicyValue.EnumSetValue,
                PolicyValue.InstantSetValue {

    PolicyValueType type();

    record BooleanValue(boolean value) implements PolicyValue {
        @Override
        public PolicyValueType type() {
            return PolicyValueType.BOOLEAN;
        }
    }

    record LongValue(long value) implements PolicyValue {
        @Override
        public PolicyValueType type() {
            return PolicyValueType.LONG;
        }
    }

    record StringValue(String value) implements PolicyValue {
        public StringValue {
            Objects.requireNonNull(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.STRING;
        }
    }

    record EnumValue(String value) implements PolicyValue {
        public EnumValue {
            Objects.requireNonNull(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.ENUM;
        }
    }

    record InstantValue(Instant value) implements PolicyValue {
        public InstantValue {
            Objects.requireNonNull(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.INSTANT;
        }
    }

    record LongSetValue(Set<Long> value) implements PolicyValue {
        public LongSetValue {
            value = immutableSortedSet(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.LONG_SET;
        }
    }

    record StringSetValue(Set<String> value) implements PolicyValue {
        public StringSetValue {
            value = immutableSortedSet(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.STRING_SET;
        }
    }

    record EnumSetValue(Set<String> value) implements PolicyValue {
        public EnumSetValue {
            value = immutableSortedSet(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.ENUM_SET;
        }
    }

    record InstantSetValue(Set<Instant> value) implements PolicyValue {
        public InstantSetValue {
            value = immutableSortedSet(value);
        }

        @Override
        public PolicyValueType type() {
            return PolicyValueType.INSTANT_SET;
        }
    }

    private static <T extends Comparable<? super T>> Set<T> immutableSortedSet(Set<T> values) {
        Objects.requireNonNull(values);
        return Collections.unmodifiableSet(new TreeSet<>(values));
    }
}
