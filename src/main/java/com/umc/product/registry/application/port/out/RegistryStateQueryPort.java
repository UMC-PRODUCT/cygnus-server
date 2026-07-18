package com.umc.product.registry.application.port.out;

import com.umc.product.registry.domain.RegistryCutoverState;

public interface RegistryStateQueryPort {

    RegistryCutoverState loadState(String registryName);
}
