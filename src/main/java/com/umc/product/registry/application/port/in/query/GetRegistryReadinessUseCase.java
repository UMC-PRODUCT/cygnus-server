package com.umc.product.registry.application.port.in.query;

import java.util.List;

import com.umc.product.registry.domain.OwnershipEnforcementMode;
import com.umc.product.registry.domain.RegistryName;

public interface GetRegistryReadinessUseCase {

    boolean isReady(RegistryName registryName);

    boolean areAllRegistriesReady();

    List<String> invalidNamespaces(RegistryName registryName);

    OwnershipEnforcementMode ownershipMode(RegistryName registryName);
}
