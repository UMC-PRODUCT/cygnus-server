package com.umc.product.demoday.application.service.command;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.annotation.Audited;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayVoteStatusUseCase;
import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayVoteStatusCommand;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.LoadDemodayVotePort;
import com.umc.product.demoday.application.port.out.SaveDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.global.exception.constant.Domain;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeDemodayVoteStatusCommandService implements ChangeDemodayVoteStatusUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final LoadDemodayVotePort loadDemodayVotePort;
    private final SaveDemodayVotePort saveDemodayVotePort;
    private final Clock clock;

    @Override
    @Audited(
        domain = Domain.DEMODAY,
        action = AuditAction.REVOKE,
        targetType = "DemodayVote",
        targetId = "#command.voteId()",
        description = "'Poll ' + #command.pollId() + '의 표를 무효화했습니다. 사유: ' + #command.reason()"
    )
    public DemodayVoteStatusInfo revoke(ChangeDemodayVoteStatusCommand command) {
        validateAdminAccess(command);
        DemodayVote vote = getVoteForUpdate(command.pollId(), command.voteId());
        vote.revoke(Instant.now(clock));

        return DemodayVoteStatusInfo.from(saveDemodayVotePort.save(vote));
    }

    @Override
    @Audited(
        domain = Domain.DEMODAY,
        action = AuditAction.RESTORE,
        targetType = "DemodayVote",
        targetId = "#command.voteId()",
        description = "'Poll ' + #command.pollId() + '의 표 무효화를 해제했습니다. 사유: ' + #command.reason()"
    )
    public DemodayVoteStatusInfo restore(ChangeDemodayVoteStatusCommand command) {
        validateAdminAccess(command);
        DemodayVote vote = getVoteForUpdate(command.pollId(), command.voteId());
        vote.restore();
        return DemodayVoteStatusInfo.from(saveDemodayVotePort.save(vote));
    }

    private void validateAdminAccess(ChangeDemodayVoteStatusCommand command) {
        DemodayPoll poll = loadDemodayPollPort.findById(command.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));
        adminAccessChecker.validateAdminAccess(command.requesterMemberId(), poll.getGisuId());
    }

    private DemodayVote getVoteForUpdate(Long pollId, Long voteId) {
        return loadDemodayVotePort.findByIdInPollForUpdate(pollId, voteId)
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_VOTE_NOT_FOUND));
    }
}
