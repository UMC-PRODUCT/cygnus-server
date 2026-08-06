package com.umc.product.authorization.application.service.command;

import java.util.Collection;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.authorization.application.service.AuthoritySnapshotCacheKeys;
import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheNamespace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthoritySnapshotCacheCommandService implements EvictAuthoritySnapshotCacheUseCase {

    private final CacheUseCase cacheUseCase;

    @Override
    public void evictByMemberId(Long memberId) {
        if (memberId == null) {
            return;
        }

        evictAfterCommit(AuthoritySnapshotCacheKeys.member(memberId));
    }

    @Override
    public void evictByMemberIds(Collection<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        memberIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .forEach(this::evictByMemberId);
    }

    private void evictAfterCommit(CacheKey cacheKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evictNow(cacheKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictNow(cacheKey);
            }
        });
    }

    private void evictNow(CacheKey cacheKey) {
        cacheUseCase.evict(CacheNamespace.AUTHORITY_SNAPSHOT, cacheKey);
    }
}
