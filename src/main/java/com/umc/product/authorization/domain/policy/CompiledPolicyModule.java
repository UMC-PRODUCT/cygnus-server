package com.umc.product.authorization.domain.policy;

import java.util.List;
import java.util.Objects;

public record CompiledPolicyModule(String id, String filename, List<CompiledPolicyStatement> statements) {
    public CompiledPolicyModule {
        Objects.requireNonNull(id);
        Objects.requireNonNull(filename);
        statements = List.copyOf(statements);
    }
}
