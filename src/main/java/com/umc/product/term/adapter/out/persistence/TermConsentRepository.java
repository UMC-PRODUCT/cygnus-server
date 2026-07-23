package com.umc.product.term.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.enums.TermType;

public interface TermConsentRepository extends JpaRepository<TermConsent, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO term_consent (member_id, term_id, term_type, agreed_at, created_at, updated_at)
        VALUES (:memberId, :termId, :termType, :agreedAt, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (member_id, term_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
        @Param("memberId") Long memberId,
        @Param("termId") Long termId,
        @Param("termType") String termType,
        @Param("agreedAt") Instant agreedAt
    );

    /**
     * 회원 ID로 동의한 약관 목록을 조회합니다.
     */
    List<TermConsent> findByMemberId(Long memberId);

    /**
     * 회원 ID와 약관 ID 목록으로 동의한 약관 목록을 조회합니다.
     */
    List<TermConsent> findByMemberIdAndTermIdIn(Long memberId, List<Long> termIds);

    /**
     * 회원 ID와 약관 타입으로 동의 정보를 조회합니다.
     */
    Optional<TermConsent> findByMemberIdAndTermType(Long memberId, TermType termType);

    /**
     * 회원 ID와 약관 ID로 동의 정보를 조회합니다.
     */
    Optional<TermConsent> findByMemberIdAndTermId(Long memberId, Long termId);

    /**
     * 회원이 특정 타입의 약관에 동의했는지 확인합니다.
     */
    boolean existsByMemberIdAndTermType(Long memberId, TermType termType);

    /**
     * 회원이 특정 약관 row 에 동의했는지 확인합니다.
     */
    boolean existsByMemberIdAndTermId(Long memberId, Long termId);
}
