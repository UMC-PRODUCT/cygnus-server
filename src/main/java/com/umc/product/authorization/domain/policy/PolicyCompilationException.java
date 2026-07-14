package com.umc.product.authorization.domain.policy;

import java.util.Objects;

public final class PolicyCompilationException extends RuntimeException {

    private final PolicyFailureCode code;

    public PolicyCompilationException(PolicyFailureCode code) {
        super("Policy compilation failed: " + Objects.requireNonNull(code).name());
        this.code = code;
    }

    public PolicyFailureCode code() {
        return code;
    }
}
