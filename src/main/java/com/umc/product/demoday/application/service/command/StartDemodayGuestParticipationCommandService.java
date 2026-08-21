package com.umc.product.demoday.application.service.command;

import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationCommand;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.GuestDemodayParticipant;
import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.HashDemodayParticipationRequestIdPort;
import com.umc.product.demoday.application.port.out.IssueDemodayParticipantTokenPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Transactional
@Service
@RequiredArgsConstructor
public class StartDemodayGuestParticipationCommandService implements StartDemodayGuestParticipationUseCase {

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    private final SaveDemodayEntryCodePort saveDemodayEntryCodePort;
    private final HashDemodayEntryCodePort hashDemodayEntryCodePort;
    private final HashDemodayParticipationRequestIdPort hashDemodayParticipationRequestIdPort;
    private final IssueDemodayParticipantTokenPort issueDemodayParticipantTokenPort;
    private final GetDemodayParticipationUseCase getDemodayParticipationUseCase;

    @Override
    public StartDemodayGuestParticipationInfo start(StartDemodayGuestParticipationCommand command) {
        DemodayPoll poll = loadDemodayPollPort.findById(command.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        String codeHash = hashDemodayEntryCodePort.hash(command.admissionCode());
        DemodayEntryCode entryCode = loadDemodayEntryCodePort.findByCodeHashForRedemption(codeHash)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_NOT_FOUND));

        if (!Objects.equals(entryCode.getPollId(), poll.getId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_POLL_MISMATCH);
        }

        if (command.requestId() != null) {
            redeemOrResume(command.requestId(), entryCode);
        } else if (!isLegacyResubmissionByExistingHolder(command, entryCode)) {
            entryCode.redeem(Instant.now());
            saveDemodayEntryCodePort.save(entryCode);
        }

        String participantToken = issueDemodayParticipantTokenPort.issue(entryCode.getId(), poll.getClosesAt());

        DemodayParticipationInfo participation = getDemodayParticipationUseCase.getParticipation(
            poll.getId(), new GuestDemodayParticipant(entryCode.getId()));

        return new StartDemodayGuestParticipationInfo(participantToken, poll.getClosesAt(), participation);
    }

    private void redeemOrResume(String requestId, DemodayEntryCode entryCode) {
        String requestIdHash = hashDemodayParticipationRequestIdPort.hash(requestId);
        if (entryCode.redeemOrResume(Instant.now(), requestIdHash)) {
            saveDemodayEntryCodePort.save(entryCode);
        }
    }

    private static boolean isLegacyResubmissionByExistingHolder(
        StartDemodayGuestParticipationCommand command, DemodayEntryCode entryCode) {

        return command.existingEntryCodeId() != null
            && command.existingEntryCodeId().equals(entryCode.getId());
    }
}
