package com.umc.product.storage.application.port.out;

import com.umc.product.storage.domain.enums.FileUsageRegistryStatus;

public interface FileUsageRegistryReadinessPort {

    FileUsageRegistryStatus getStatus();

    default boolean isReady() {
        return getStatus() == FileUsageRegistryStatus.READY;
    }
}
