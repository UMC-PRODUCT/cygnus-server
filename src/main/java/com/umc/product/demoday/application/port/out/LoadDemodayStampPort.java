package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayStamp;

public interface LoadDemodayStampPort {

    Optional<DemodayStamp> findById(Long stampId);

    Optional<DemodayStamp> findByMemberIdAndBoothId(Long memberId, Long boothId);

    Optional<DemodayStamp> findByEntryCodeIdAndBoothId(Long entryCodeId, Long boothId);

    List<DemodayStamp> listByMemberId(Long memberId);

    List<DemodayStamp> listByEntryCodeId(Long entryCodeId);
}
