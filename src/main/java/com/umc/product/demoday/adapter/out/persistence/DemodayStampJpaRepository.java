package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.demoday.domain.DemodayStamp;

public interface DemodayStampJpaRepository extends JpaRepository<DemodayStamp, Long> {

    Optional<DemodayStamp> findByMemberIdAndBoothId(Long memberId, Long boothId);

    Optional<DemodayStamp> findByEntryCodeIdAndBoothId(Long entryCodeId, Long boothId);

    List<DemodayStamp> findAllByMemberId(Long memberId, Sort sort);

    List<DemodayStamp> findAllByEntryCodeId(Long entryCodeId, Sort sort);

    @Query("""
        SELECT COUNT(stamp)
        FROM DemodayStamp stamp
        WHERE stamp.memberId = :memberId
          AND stamp.revokedAt IS NULL
          AND stamp.boothId IN (
              SELECT booth.id
              FROM DemodayBooth booth
              WHERE booth.pollId = :pollId
          )
        """)
    int countActiveMemberStamps(@Param("pollId") Long pollId, @Param("memberId") Long memberId);

    @Query("""
        SELECT COUNT(stamp)
        FROM DemodayStamp stamp
        WHERE stamp.entryCodeId = :entryCodeId
          AND stamp.revokedAt IS NULL
          AND stamp.boothId IN (
              SELECT booth.id
              FROM DemodayBooth booth
              WHERE booth.pollId = :pollId
          )
        """)
    int countActiveVisitorStamps(@Param("pollId") Long pollId, @Param("entryCodeId") Long entryCodeId);

    Optional<DemodayStamp> findFirstByMemberIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long memberId);

    Optional<DemodayStamp> findFirstByEntryCodeIdAndRevokedAtIsNullOrderByCreatedAtDesc(Long entryCodeId);
}
