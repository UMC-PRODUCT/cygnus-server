package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public sealed interface PolicyOperand permits PolicyOperand.Attribute, PolicyOperand.Literal {

    PolicyValueType type();

    record Attribute(String name, PolicyValueType type) implements PolicyOperand {
        public Attribute {
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
        }
    }

    record Literal(PolicyValue value) implements PolicyOperand {
        public Literal {
            Objects.requireNonNull(value);
        }

        @Override
        public PolicyValueType type() {
            return value.type();
        }
    }
}
