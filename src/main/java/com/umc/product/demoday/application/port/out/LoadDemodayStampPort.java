package com.umc.product.demoday.application.port.out;

import java.util.List;
import java.util.Optional;

import com.umc.product.demoday.domain.DemodayStamp;

public interface LoadDemodayStampPort {

    Optional<DemodayStamp> findById(Long stampId);

    Optional<DemodayStamp> findMemberStamp(Long memberId, Long boothId);

    Optional<DemodayStamp> findVisitorStamp(Long entryCodeId, Long boothId);

    List<DemodayStamp> listMemberStamps(Long memberId);

    List<DemodayStamp> listVisitorStamps(Long entryCodeId);

    int countActiveMemberStamps(Long pollId, Long memberId);

    int countActiveVisitorStamps(Long pollId, Long entryCodeId);

    Optional<DemodayStamp> findLatestActiveMemberStamp(Long memberId);

    Optional<DemodayStamp> findLatestActiveVisitorStamp(Long entryCodeId);
}
