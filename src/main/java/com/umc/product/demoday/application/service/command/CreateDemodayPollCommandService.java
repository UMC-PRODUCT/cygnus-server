package com.umc.product.demoday.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayPollCommand;
import com.umc.product.demoday.application.port.out.SaveDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayPoll;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateDemodayPollCommandService implements CreateDemodayPollUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final SaveDemodayPollPort saveDemodayPollPort;

    @Override
    public Long create(CreateDemodayPollCommand command) {
        adminAccessChecker.validateAdminAccess(command.memberId(), command.gisuId());

        DemodayPoll demodayPoll = DemodayPoll.create(
            command.gisuId(), command.name(), command.opensAt(), command.closesAt());
        DemodayPoll savedPoll = saveDemodayPollPort.save(demodayPoll);
        return savedPoll.getId();
    }
}
