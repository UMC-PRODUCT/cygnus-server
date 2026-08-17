package com.umc.product.demoday.application.service.query;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayPollInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayStampInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DemodayPollQueryService implements
    ListDemodayPollUseCase, GetDemodayParticipationUseCase, ListDemodayBoothUseCase {

    private static final int REQUIRED_STAMP_COUNT = 6;

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayStampPort loadDemodayStampPort;
    private final LoadDemodayVotePort loadDemodayVotePort;

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

        if (participant.participantType() != DemodayParticipantType.MEMBER) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_PARTICIPATION_UNSUPPORTED);
        }

        Set<Long> boothIds = loadDemodayBoothPort.listByPollId(pollId)
            .stream()
            .map(DemodayBooth::getId)
            .collect(toUnmodifiableSet());

        List<DemodayStampInfo> stamps = loadDemodayStampPort.listMemberStamps(participant.participantId())
            .stream()
            .filter(stamp -> !stamp.isRevoked())
            .filter(stamp -> boothIds.contains(stamp.getBoothId()))
            .map(this::toStampInfo)
            .toList();

        boolean hasVoted = loadDemodayVotePort.findMemberVote(pollId, participant.participantId())
            .filter(vote -> !vote.isRevoked())
            .isPresent();

        int stampCount = stamps.size();

        return new DemodayParticipationInfo(
                pollId,
                participant.participantType(),
                stampCount,
                REQUIRED_STAMP_COUNT,
                stamps,
                null,
                hasVoted,
                stampCount >= REQUIRED_STAMP_COUNT && !hasVoted
        );
    }

    @Override
    public List<DemodayBoothInfo> listBooths(Long pollId) {
        validatePollExists(pollId);

        return loadDemodayBoothPort.listByPollId(pollId)
            .stream()
            .map(booth -> new DemodayBoothInfo(
                booth.getId(),
                booth.getProjectId(),
                booth.getDisplayName()
            ))
            .toList();
    }

    private void validatePollExists(Long pollId) {
        loadDemodayPollPort.findById(pollId)
                .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
    }

    private DemodayStampInfo toStampInfo(DemodayStamp stamp) {
        return new DemodayStampInfo(stamp.getBoothId(), stamp.getCreatedAt());
    }
}
