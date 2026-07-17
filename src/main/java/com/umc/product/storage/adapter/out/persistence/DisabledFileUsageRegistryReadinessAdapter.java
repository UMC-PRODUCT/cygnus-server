package com.umc.product.storage.adapter.out.persistence;

import com.umc.product.storage.application.port.out.FileUsageRegistryReadinessPort;
import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

public class DisabledFileUsageRegistryReadinessAdapter implements FileUsageRegistryReadinessPort {

    @Override
    public FileUsageRegistryStatus getStatus() {
        return FileUsageRegistryStatus.DISABLED;
    }
}
