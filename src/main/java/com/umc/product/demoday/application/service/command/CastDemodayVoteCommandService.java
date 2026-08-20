package com.umc.product.demoday.application.service.command;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CastDemodayVoteUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CastDemodayVoteCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.demoday.application.port.out.DemodayVoteAuthorizationTokenClaims;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayVoteAuthorizationValidator;
import com.umc.product.demoday.application.service.DemodayVoteTargetValidator;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CastDemodayVoteCommandService implements CastDemodayVoteUseCase {

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    private final LoadDemodayVotePort loadDemodayVotePort;
    private final SaveDemodayVotePort saveDemodayVotePort;
    private final DemodayVoteAuthorizationValidator demodayVoteAuthorizationValidator;
    private final DemodayVoteTargetValidator demodayVoteTargetValidator;
    private final Clock clock;

    @Override
    public DemodayVoteInfo cast(CastDemodayVoteCommand command) {
        Instant now = clock.instant();
        DemodayPoll poll = loadPoll(command.pollId());
        poll.validateVotingAvailable(now);

        DemodayVoteAuthorizationTokenClaims claims = demodayVoteAuthorizationValidator.validate(
            command.pollId(), command.participant(), command.voteAuthorizationToken());
        DemodayBooth targetBooth = loadBooth(claims.boothId());
        validateSamePoll(command.pollId(), targetBooth);
        targetBooth.validateVoteTarget();
        demodayVoteTargetValidator.validateEligibleBooth(targetBooth, command.participant());
        validateVoteSlotUnused(command.pollId(), command.participant());

        DemodayVote savedVote = saveDemodayVotePort.save(createVote(command.participant(), targetBooth));

        return new DemodayVoteInfo(
            savedVote.getId(),
            savedVote.getPollId(),
            DemodayBoothInfo.from(targetBooth),
            savedVote.getCreatedAt()
        );
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

    private DemodayVote createVote(DemodayParticipant participant, DemodayBooth targetBooth) {
        return switch (participant.participantType()) {
            case MEMBER -> DemodayVote.forMember(targetBooth.getPollId(), participant.participantId(), targetBooth);
            case GUEST -> DemodayVote.forVisitor(
                targetBooth.getPollId(), loadEntryCode(participant.participantId()), targetBooth);
        };
    }

    private DemodayEntryCode loadEntryCode(Long entryCodeId) {
        return loadDemodayEntryCodePort.findById(entryCodeId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_NOT_FOUND));
    }
}
