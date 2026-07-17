package com.umc.product.storage.adapter.out.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

@Component
@ConditionalOnMissingBean(FileUsageRegistryReadinessPort.class)
public class DisabledFileUsageRegistryReadinessAdapter implements FileUsageRegistryReadinessPort {

    @Override
    public FileUsageRegistryStatus getStatus() {
        return FileUsageRegistryStatus.DISABLED;
    }
}
