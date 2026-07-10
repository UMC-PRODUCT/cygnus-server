package com.umc.product.notification.adapter.out.persistentce;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.notification.domain.FcmToken;

public interface FcmJpaRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByMemberIdAndFcmToken(Long memberId, String fcmToken);

    List<FcmToken> findAllByMemberIdAndIsActiveTrue(Long memberId);

    List<FcmToken> findAllByMemberIdInAndIsActiveTrue(List<Long> memberIds);

    List<FcmToken> findAllByFcmTokenAndIsActiveTrue(String fcmToken);

    List<FcmToken> findAllByIdInAndIsActiveTrue(List<Long> ids);

    @Query("""
        SELECT f
        FROM FcmToken f
        WHERE f.isActive = true
          AND (f.lastValidatedAt IS NULL OR f.lastValidatedAt <= :validatedBefore)
        ORDER BY f.lastValidatedAt ASC NULLS FIRST, f.id ASC
        """)
    List<FcmToken> findActiveValidationTargets(
        @Param("validatedBefore") Instant validatedBefore,
        Pageable pageable
    );
}
