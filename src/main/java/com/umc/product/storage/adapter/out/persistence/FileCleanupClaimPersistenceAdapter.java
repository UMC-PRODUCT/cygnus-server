package com.umc.product.storage.adapter.out.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.storage.application.port.out.FileCleanupClaimPort;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaim;
import com.umc.product.storage.application.port.out.dto.FileCleanupClaimCriteria;
import com.umc.product.storage.application.port.out.dto.FileCleanupFailure;
import com.umc.product.storage.domain.FileMetadata;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileCleanupClaimPersistenceAdapter implements FileCleanupClaimPort {

    private static final String CLAIM_CANDIDATES_SQL = """
        SELECT metadata.*
        FROM file_metadata metadata
        WHERE metadata.cleanup_failed_at IS NULL
          AND NOT EXISTS (
              SELECT 1
              FROM file_usage usage
              WHERE usage.file_id = metadata.id
          )
          AND (
              (
                  metadata.cleanup_claim_token IS NOT NULL
                  AND metadata.cleanup_claimed_at <= :claimExpiredBefore
              )
              OR (
                  metadata.cleanup_claim_token IS NULL
                  AND (
                      (
                          metadata.cleanup_next_attempt_at IS NOT NULL
                          AND metadata.cleanup_next_attempt_at <= :now
                      )
                      OR (
                          metadata.cleanup_next_attempt_at IS NULL
                          AND (
                              (
                                  metadata.confirmed_at IS NULL
                                  AND metadata.is_uploaded = false
                                  AND metadata.created_at <= :pendingCreatedBefore
                              )
                              OR (
                                  metadata.confirmed_at IS NOT NULL
                                  AND metadata.unreferenced_at IS NOT NULL
                                  AND metadata.unreferenced_at <= :unreferencedBefore
                              )
                          )
                      )
                  )
              )
          )
        ORDER BY COALESCE(
            metadata.cleanup_next_attempt_at,
            metadata.cleanup_claimed_at,
            metadata.unreferenced_at,
            metadata.created_at
        ), metadata.id
        FOR UPDATE OF metadata SKIP LOCKED
        LIMIT :batchSize
        """;

    private final EntityManager entityManager;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileUsageRepository fileUsageRepository;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public List<FileCleanupClaim> claimBatch(FileCleanupClaimCriteria criteria) {
        Query query = entityManager.createNativeQuery(CLAIM_CANDIDATES_SQL, FileMetadata.class)
            .setParameter("now", criteria.now())
            .setParameter("pendingCreatedBefore", criteria.pendingCreatedBefore())
            .setParameter("unreferencedBefore", criteria.unreferencedBefore())
            .setParameter("claimExpiredBefore", criteria.claimExpiredBefore())
            .setParameter("batchSize", criteria.batchSize());
        List<?> queryResults = query.getResultList();
        List<FileMetadata> candidates = queryResults.stream()
            .map(FileMetadata.class::cast)
            .toList();
        List<FileCleanupClaim> claims = new ArrayList<>(candidates.size());

        for (FileMetadata metadata : candidates) {
            if (metadata.getCleanupAttempts() >= criteria.maxAttempts()) {
                metadata.quarantineCleanup(criteria.now());
                continue;
            }

            UUID token = UUID.randomUUID();
            if (metadata.hasCleanupClaim()) {
                metadata.reclaimCleanup(token, criteria.now());
            } else {
                metadata.claimCleanup(token, criteria.now());
            }
            claims.add(new FileCleanupClaim(
                metadata.getId(),
                metadata.getStorageKey(),
                token,
                metadata.getCleanupAttempts()
            ));
        }

        entityManager.flush();
        return List.copyOf(claims);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean finalizeDeletion(FileCleanupClaim claim) {
        List<FileMetadata> locked = fileMetadataRepository.findAllByIdInOrderByIdForUpdate(List.of(claim.fileId()));
        if (locked.isEmpty()) {
            return false;
        }

        FileMetadata metadata = locked.getFirst();
        if (!metadata.matchesCleanupClaim(claim.token())) {
            return false;
        }
        if (fileUsageRepository.countByFileId(claim.fileId()) != 0L) {
            return false;
        }

        fileMetadataRepository.delete(metadata);
        fileMetadataRepository.flush();
        return true;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean recordFailure(FileCleanupFailure failure) {
        FileCleanupClaim claim = failure.claim();
        List<FileMetadata> locked = fileMetadataRepository.findAllByIdInOrderByIdForUpdate(List.of(claim.fileId()));
        if (locked.isEmpty()) {
            return false;
        }

        FileMetadata metadata = locked.getFirst();
        if (!metadata.recordCleanupFailure(
            claim.token(),
            failure.failedAt(),
            failure.nextAttemptAt(),
            failure.maxAttempts()
        )) {
            return false;
        }

        fileMetadataRepository.flush();
        return true;
    }
}
