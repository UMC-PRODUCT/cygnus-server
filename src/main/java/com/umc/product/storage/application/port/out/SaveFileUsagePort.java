package com.umc.product.storage.application.port.out;

import java.util.Set;

public interface SaveFileUsagePort {

    void addUsages(Long ownerId, Set<String> fileIds);

    void removeUsages(Long ownerId, Set<String> fileIds);
}
