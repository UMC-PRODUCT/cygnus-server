package com.umc.product.registry.application.port.out;

import com.umc.product.registry.domain.RegistryName;
import com.umc.product.registry.domain.RegistryNamespaceCoverage;

public interface RegistryNamespaceCoveragePort {

    RegistryName registryName();

    RegistryNamespaceCoverage loadCoverage();
}
