package com.umc.product.storage.application.port.out;

import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.storage.domain.FileUsageOwner;

public interface LockFileUsageOwnerPort {

    /**
     * 좌표의 serialization anchor를 보장하고 비관적 쓰기 lock으로 반환합니다.
     * 호출자는 usage 변경 transaction 안에서 이 메서드를 호출해야 합니다.
     */
    FileUsageOwner lockOrCreateOwner(FileUsageCoordinate coordinate);
}
