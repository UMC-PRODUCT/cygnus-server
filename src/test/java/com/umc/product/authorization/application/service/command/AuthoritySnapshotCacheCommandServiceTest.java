package com.umc.product.authorization.application.service.command;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.umc.product.global.cache.application.port.in.CacheUseCase;
import com.umc.product.global.cache.domain.CacheKey;
import com.umc.product.global.cache.domain.CacheNamespace;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthoritySnapshotCacheCommandService")
class AuthoritySnapshotCacheCommandServiceTest {

    @Mock
    CacheUseCase cacheUseCase;

    @Test
    @DisplayName("회원 ID로 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_by_member_id() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);

        sut.evictByMemberId(10L);

        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:10"));
    }

    @Test
    @DisplayName("회원 ID 목록으로 권한 snapshot 캐시를 중복 없이 제거한다")
    void evict_authority_snapshot_by_member_ids_without_duplicates() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);

        sut.evictByMemberIds(List.of(10L, 20L, 10L));

        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:10"));
        verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:20"));
    }

    @Test
    @DisplayName("트랜잭션 동기화가 활성화되어 있으면 commit 이후 권한 snapshot 캐시를 제거한다")
    void evict_authority_snapshot_after_commit_when_transaction_synchronization_is_active() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);
        TransactionSynchronizationManager.initSynchronization();
        try {
            sut.evictByMemberId(10L);

            verifyNoInteractions(cacheUseCase);

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

            verify(cacheUseCase).evict(CacheNamespace.AUTHORITY_SNAPSHOT, CacheKey.from("member:10"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("트랜잭션이 rollback되면 권한 snapshot 캐시를 제거하지 않는다")
    void does_not_evict_authority_snapshot_after_rollback() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);
        TransactionSynchronizationManager.initSynchronization();
        try {
            sut.evictByMemberId(10L);

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verifyNoInteractions(cacheUseCase);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("회원 ID가 없으면 캐시를 제거하지 않는다")
    void does_not_evict_without_member_id() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);

        sut.evictByMemberId(null);

        verifyNoInteractions(cacheUseCase);
    }

    @Test
    @DisplayName("회원 ID 목록이 null이거나 비어 있으면 캐시를 제거하지 않는다")
    void does_not_evict_without_member_ids() {
        AuthoritySnapshotCacheCommandService sut = new AuthoritySnapshotCacheCommandService(cacheUseCase);

        sut.evictByMemberIds(null);
        sut.evictByMemberIds(List.of());

        verifyNoInteractions(cacheUseCase);
    }
}
