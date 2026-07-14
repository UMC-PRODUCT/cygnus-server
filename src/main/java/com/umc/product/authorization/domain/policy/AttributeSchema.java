package com.umc.product.authorization.domain.policy;

import java.util.Objects;
import java.util.Set;

public record AttributeSchema(String attributeName, PolicyValueType type, Set<String> enumSymbols) {
    public AttributeSchema {
        Objects.requireNonNull(attributeName);
        Objects.requireNonNull(type);
        enumSymbols = Set.copyOf(enumSymbols);
        boolean enumType = type == PolicyValueType.ENUM || type == PolicyValueType.ENUM_SET;
        if (enumType && enumSymbols.isEmpty()) {
            throw new IllegalArgumentException("Enum attribute requires symbols");
        }
        if (!enumType && !enumSymbols.isEmpty()) {
            throw new IllegalArgumentException("Only enum attributes may define symbols");
        }
    }
}
