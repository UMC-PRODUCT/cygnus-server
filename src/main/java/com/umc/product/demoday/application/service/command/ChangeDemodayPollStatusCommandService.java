package com.umc.product.demoday.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.dto.ChangeDemodayPollStatusCommand;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ChangeDemodayPollStatusCommandService implements ChangeDemodayPollStatusUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final SaveDemodayPollPort saveDemodayPollPort;
    private final LoadDemodayPollPort loadDemodayPollPort;

    @Override
    public void changeStatus(ChangeDemodayPollStatusCommand command) {
        DemodayPoll poll = getDemodayPoll(command);

        switch (command.status()) {
            case READY -> throw new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_INVALID_STATUS_TRANSITION);
            case OPEN -> poll.open();
            case CLOSED -> poll.close();
        }

        saveDemodayPollPort.save(poll);
    }

    private DemodayPoll getDemodayPoll(ChangeDemodayPollStatusCommand command) {
        DemodayPoll poll = loadDemodayPollPort.findById(command.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(command.memberId(), poll.getGisuId());
        return poll;
    }
}
