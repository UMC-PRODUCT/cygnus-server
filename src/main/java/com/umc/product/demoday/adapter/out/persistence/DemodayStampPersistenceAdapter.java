package com.umc.product.demoday.adapter.out.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.SaveDemodayStampPort;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemodayStampPersistenceAdapter implements LoadDemodayStampPort, SaveDemodayStampPort {

    private static final Sort STAMP_ORDER = Sort.by(
        Sort.Order.desc("createdAt"),
        Sort.Order.desc("id")
    );
    private static final Map<String, DemodayErrorCode> ERROR_CODES_BY_CONSTRAINT = Map.of(
        "uk_demoday_stamp_member_booth", DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED,
        "uk_demoday_stamp_entry_code_booth", DemodayErrorCode.DEMODAY_STAMP_ALREADY_COLLECTED
    );

    private final DemodayStampJpaRepository repository;

    @Override
    public Optional<DemodayStamp> findById(Long stampId) {
        return repository.findById(stampId);
    }

    @Override
    public Optional<DemodayStamp> findMemberStamp(Long memberId, Long boothId) {
        return repository.findByMemberIdAndBoothId(memberId, boothId);
    }

    @Override
    public Optional<DemodayStamp> findVisitorStamp(Long entryCodeId, Long boothId) {
        return repository.findByEntryCodeIdAndBoothId(entryCodeId, boothId);
    }

    @Override
    public List<DemodayStamp> listMemberStamps(Long memberId) {
        return repository.findAllByMemberId(memberId, STAMP_ORDER);
    }

    @Override
    public List<DemodayStamp> listVisitorStamps(Long entryCodeId) {
        return repository.findAllByEntryCodeId(entryCodeId, STAMP_ORDER);
    }

    @Override
    public int countActiveMemberStamps(Long pollId, Long memberId) {
        return repository.countActiveMemberStamps(pollId, memberId);
    }

    @Override
    public int countActiveVisitorStamps(Long pollId, Long entryCodeId) {
        return repository.countActiveVisitorStamps(pollId, entryCodeId);
    }

    @Override
    public Optional<DemodayStamp> findLatestActiveMemberStamp(Long memberId) {
        return repository.findFirstByMemberIdAndRevokedAtIsNullOrderByCreatedAtDesc(memberId);
    }

    @Override
    public Optional<DemodayStamp> findLatestActiveVisitorStamp(Long entryCodeId) {
        return repository.findFirstByEntryCodeIdAndRevokedAtIsNullOrderByCreatedAtDesc(entryCodeId);
    }

    @Override
    public DemodayStamp save(DemodayStamp stamp) {
        try {
            return repository.saveAndFlush(stamp);
        } catch (DataIntegrityViolationException exception) {
            throw DemodayConstraintViolationTranslator.translate(exception, ERROR_CODES_BY_CONSTRAINT);
        }
    }
}
