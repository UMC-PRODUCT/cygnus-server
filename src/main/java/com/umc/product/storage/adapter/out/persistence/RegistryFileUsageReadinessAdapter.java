package com.umc.product.storage.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.in.query.GetRegistryReadinessUseCase;
import com.umc.product.registry.domain.RegistryName;
import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RegistryFileUsageReadinessAdapter implements FileUsageRegistryReadinessPort {

    private final GetRegistryReadinessUseCase registryReadiness;

    @Override
    public FileUsageRegistryStatus getStatus() {
        return registryReadiness.isReady(RegistryName.STORAGE_USAGE)
            ? FileUsageRegistryStatus.READY
            : FileUsageRegistryStatus.DISABLED;
    }
}
