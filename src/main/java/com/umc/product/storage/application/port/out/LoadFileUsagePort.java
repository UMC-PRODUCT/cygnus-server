package com.umc.product.storage.application.port.out;

import java.util.Set;

public interface LoadFileUsagePort {

    Set<String> findFileIdsByOwnerId(Long ownerId);

    long countByFileId(String fileId);
}
