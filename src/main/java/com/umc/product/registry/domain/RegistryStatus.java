package com.umc.product.registry.domain;

public enum RegistryStatus {
    DISABLED,
    BACKFILLING,
    VALIDATED,
    READY,
    BLOCKED;

    public boolean canProgressTo(RegistryStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case DISABLED -> target == BACKFILLING;
            case BACKFILLING -> target == VALIDATED || target == BLOCKED;
            case VALIDATED -> target == READY || target == BLOCKED;
            case READY -> target == BLOCKED;
            case BLOCKED -> target == DISABLED;
        };
    }
}
