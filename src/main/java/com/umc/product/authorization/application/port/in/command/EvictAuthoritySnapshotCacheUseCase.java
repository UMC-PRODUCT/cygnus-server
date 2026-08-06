package com.umc.product.authorization.application.port.in.command;

import java.util.Collection;

public interface EvictAuthoritySnapshotCacheUseCase {

    void evictByMemberId(Long memberId);

    void evictByMemberIds(Collection<Long> memberIds);
}
