package com.umc.product.authorization.domain.policy;

public enum PolicyValueType {
    BOOLEAN,
    LONG,
    STRING,
    ENUM,
    INSTANT,
    LONG_SET,
    STRING_SET,
    ENUM_SET,
    INSTANT_SET;

    public boolean isScalar() {
        return switch (this) {
            case BOOLEAN, LONG, STRING, ENUM, INSTANT -> true;
            case LONG_SET, STRING_SET, ENUM_SET, INSTANT_SET -> false;
        };
    }

    public boolean isSet() {
        return !isScalar();
    }

    public PolicyValueType elementType() {
        return switch (this) {
            case LONG_SET -> LONG;
            case STRING_SET -> STRING;
            case ENUM_SET -> ENUM;
            case INSTANT_SET -> INSTANT;
            default -> throw new IllegalStateException("Scalar policy value has no element type");
        };
    }

    public PolicyValueType setType() {
        return switch (this) {
            case LONG -> LONG_SET;
            case STRING -> STRING_SET;
            case ENUM -> ENUM_SET;
            case INSTANT -> INSTANT_SET;
            default -> throw new IllegalStateException("Policy value has no matching set type");
        };
    }
}
