package com.umc.product.demoday.application.service.command;

import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateDemodayPollCommandService implements CreateDemodayPollUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final SaveDemodayPollPort saveDemodayPollPort;

    @Override
    public Long create(CreateDemodayPollCommand command) {
        adminAccessChecker.validateAdminAccess(command.memberId(), command.gisuId());

        DemodayPoll demodayPoll = DemodayPoll.create(command.gisuId(), command.name(), command.opensAt(), command.closesAt());
        DemodayPoll savedPoll = saveDemodayPollPort.save(demodayPoll);
        return savedPoll.getId();
    }
}
