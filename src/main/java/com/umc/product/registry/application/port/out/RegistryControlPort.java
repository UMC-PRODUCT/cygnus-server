package com.umc.product.registry.application.port.out;

import java.time.Instant;
import java.util.List;

import com.umc.product.registry.domain.RegistryBackfillCheckpoint;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryStatus;

public interface RegistryControlPort {

    RegistryCutoverState loadState(String registryName);

    boolean transition(
        String registryName,
        RegistryStatus expected,
        RegistryStatus target,
        Instant verifiedAt,
        String details
    );

    RegistryBackfillCheckpoint loadCheckpoint(String registryName, String sourceName);

    void saveCheckpoint(RegistryBackfillCheckpoint checkpoint);

    void resetCheckpoints(String registryName, List<String> sourceNames);
}
