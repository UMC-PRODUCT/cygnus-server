package com.umc.product.demoday.application.service.query;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayVoteReceiptInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayVoteTargetValidator;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.demoday.domain.policy.DemodayParticipationPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayPollQueryService implements
    ListDemodayPollUseCase, GetDemodayParticipationUseCase, ListDemodayBoothUseCase {

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayStampPort loadDemodayStampPort;
    private final LoadDemodayVotePort loadDemodayVotePort;
    private final DemodayVoteTargetValidator demodayVoteTargetValidator;

    @Override
    public List<DemodayPollInfo> listPolls() {
        return loadDemodayPollPort.listAll().stream()
                .map(poll -> new DemodayPollInfo(
                        poll.getId(),
                        poll.getName(),
                        poll.getOpensAt(),
                        poll.getClosesAt(),
                        poll.getStatus()
                ))
                .toList();
    }

    @Override
    public DemodayParticipationInfo getParticipation(Long pollId, DemodayParticipant participant) {
        validatePollExists(pollId);

        Map<Long, DemodayBooth> boothsById = loadDemodayBoothPort.listByPollId(pollId)
            .stream()
            .collect(toUnmodifiableMap(DemodayBooth::getId, identity()));

        List<DemodayStampInfo> stamps = loadStamps(participant)
            .stream()
            .filter(stamp -> !stamp.isRevoked())
            .filter(stamp -> boothsById.containsKey(stamp.getBoothId()))
            .map(this::toStampInfo)
            .toList();

        Optional<DemodayVote> vote = findVote(pollId, participant);
        Optional<DemodayVote> activeVote = vote.filter(existingVote -> !existingVote.isRevoked());
        boolean hasActiveVote = activeVote.isPresent();
        boolean hasUsedVoteSlot = vote.isPresent();
        DemodayVoteReceiptInfo activeVoteReceipt = activeVote
            .map(existingVote -> toVoteReceiptInfo(existingVote, boothsById))
            .orElse(null);

        int stampCount = stamps.size();

        return new DemodayParticipationInfo(
                pollId,
                participant.participantType(),
                stampCount,
                DemodayParticipationPolicy.requiredStampCount(),
                stamps,
                null,
                hasActiveVote,
                hasUsedVoteSlot,
                DemodayParticipationPolicy.canRequestVoteAuthorization(stampCount, hasUsedVoteSlot),
                activeVoteReceipt
        );
    }

    @Override
    public List<DemodayBoothInfo> listBooths(Long pollId, DemodayParticipant participant) {
        validatePollExists(pollId);

        List<DemodayBooth> booths = loadDemodayBoothPort.listByPollId(pollId);

        return demodayVoteTargetValidator.filterEligibleBooths(booths, participant)
            .stream()
            .filter(DemodayBooth::isProjectBooth)
            .map(DemodayBoothInfo::from)
            .toList();
    }

    private void validatePollExists(Long pollId) {
        loadDemodayPollPort.findById(pollId)
                .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
    }

    /**
     * 참여자 유형별 조회 포트가 다르다(회원=memberId, 게스트=entryCodeId). 참여 상태는 저장된 값이 아니라
     * 스탬프·투표 기록에서 매 요청마다 파생되므로 유형이 늘어날 경우 분기만 추가하면 된다.
     */
    private List<DemodayStamp> loadStamps(DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayStampPort.listMemberStamps(participant.participantId());
            case GUEST -> loadDemodayStampPort.listVisitorStamps(participant.participantId());
        };
    }

    private Optional<DemodayVote> findVote(Long pollId, DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayVotePort.findMemberVote(pollId, participant.participantId());
            case GUEST -> loadDemodayVotePort.findVisitorVote(participant.participantId());
        };
    }

    private DemodayStampInfo toStampInfo(DemodayStamp stamp) {
        return new DemodayStampInfo(stamp.getBoothId(), stamp.getCreatedAt());
    }

    private DemodayVoteReceiptInfo toVoteReceiptInfo(
        DemodayVote vote,
        Map<Long, DemodayBooth> boothsById
    ) {
        DemodayBooth selectedBooth = Optional.ofNullable(boothsById.get(vote.getTargetBoothId()))
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_NOT_FOUND));

        return new DemodayVoteReceiptInfo(
            vote.getId(),
            DemodayBoothInfo.from(selectedBooth),
            vote.getCreatedAt()
        );
    }
}
