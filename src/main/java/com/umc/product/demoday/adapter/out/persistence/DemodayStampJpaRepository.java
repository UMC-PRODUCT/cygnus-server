package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.product.demoday.domain.DemodayStamp;

public interface DemodayStampJpaRepository extends JpaRepository<DemodayStamp, Long> {

    Optional<DemodayStamp> findByMemberIdAndBoothId(Long memberId, Long boothId);

    Optional<DemodayStamp> findByEntryCodeIdAndBoothId(Long entryCodeId, Long boothId);

    List<DemodayStamp> findAllByMemberIdOrderByCreatedAtDescIdDesc(Long memberId);

    List<DemodayStamp> findAllByEntryCodeIdOrderByCreatedAtDescIdDesc(Long entryCodeId);
}
