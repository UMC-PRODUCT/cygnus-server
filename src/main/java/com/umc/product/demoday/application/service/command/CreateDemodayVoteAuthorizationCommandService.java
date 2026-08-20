package com.umc.product.demoday.application.service.command;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CreateDemodayVoteAuthorizationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayVoteAuthorizationCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.GenerateDemodayVoteAuthorizationPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayStampPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayVoteQrCredentialValidator;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayStamp;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.demoday.domain.policy.DemodayParticipationPolicy;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreateDemodayVoteAuthorizationCommandService implements CreateDemodayVoteAuthorizationUseCase {

    private static final Duration AUTHORIZATION_TTL = Duration.ofMinutes(5);

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayStampPort loadDemodayStampPort;
    private final LoadDemodayVotePort loadDemodayVotePort;
    private final DemodayVoteQrCredentialValidator demodayVoteQrCredentialValidator;
    private final GenerateDemodayVoteAuthorizationPort generateDemodayVoteAuthorizationPort;
    private final Clock clock;

    @Override
    public DemodayVoteAuthorizationInfo create(CreateDemodayVoteAuthorizationCommand command) {
        Instant issuedAt = clock.instant();
        DemodayPoll poll = loadPoll(command.pollId());
        poll.validateVotingAvailable(issuedAt);
        demodayVoteQrCredentialValidator.validate(command.pollId(), command.qrToken());

        DemodayBooth booth = loadBooth(command.boothId());
        validateSamePoll(command.pollId(), booth);
        booth.validateVoteTarget();
        validateVoteSlotUnused(command.pollId(), command.participant());
        validateRequiredStamps(command.pollId(), command.participant());

        Instant expiresAt = issuedAt.plus(AUTHORIZATION_TTL);
        String token = generateDemodayVoteAuthorizationPort.generate(
            command.participant(),
            command.pollId(),
            booth.getId(),
            issuedAt,
            expiresAt
        );

        return new DemodayVoteAuthorizationInfo(token, DemodayBoothInfo.from(booth), expiresAt);
    }

    private DemodayPoll loadPoll(Long pollId) {
        return loadDemodayPollPort.findById(pollId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
    }

    private DemodayBooth loadBooth(Long boothId) {
        return loadDemodayBoothPort.findById(boothId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_NOT_FOUND));
    }

    private void validateSamePoll(Long pollId, DemodayBooth booth) {
        if (!pollId.equals(booth.getPollId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_POLL_MISMATCH);
        }
    }

    private void validateVoteSlotUnused(Long pollId, DemodayParticipant participant) {
        boolean hasUsedVoteSlot = switch (participant.participantType()) {
            case MEMBER -> loadDemodayVotePort.findMemberVote(pollId, participant.participantId()).isPresent();
            case GUEST -> loadDemodayVotePort.findVisitorVote(participant.participantId()).isPresent();
        };

        if (hasUsedVoteSlot) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_ALREADY_CAST);
        }
    }

    private void validateRequiredStamps(Long pollId, DemodayParticipant participant) {
        Set<Long> boothIds = loadDemodayBoothPort.listByPollId(pollId).stream()
            .map(DemodayBooth::getId)
            .collect(toUnmodifiableSet());

        long activeStampCount = loadStamps(participant).stream()
            .filter(stamp -> !stamp.isRevoked())
            .filter(stamp -> boothIds.contains(stamp.getBoothId()))
            .count();

        if (!DemodayParticipationPolicy.hasRequiredStamps(activeStampCount)) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_INSUFFICIENT_STAMPS);
        }
    }

    private List<DemodayStamp> loadStamps(DemodayParticipant participant) {
        return switch (participant.participantType()) {
            case MEMBER -> loadDemodayStampPort.listMemberStamps(participant.participantId());
            case GUEST -> loadDemodayStampPort.listVisitorStamps(participant.participantId());
        };
    }
}
