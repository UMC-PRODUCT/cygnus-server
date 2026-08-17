package com.umc.product.demoday.application.service.command;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CreateDemodayEntryCodeUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodeCommand;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;
import com.umc.product.demoday.application.port.out.GenerateDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.HashDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SaveDemodayEntryCodePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Transactional
@Service
@RequiredArgsConstructor
public class CreateDemodayEntryCodeCommandService implements CreateDemodayEntryCodeUseCase {

    private final LoadDemodayPollPort loadDemodayPollPort;
    private final SaveDemodayEntryCodePort saveDemodayEntryCodePort;
    private final DemodayAdminAccessChecker demodayAdminAccessChecker;
    private final GenerateDemodayEntryCodePort generateDemodayEntryCodePort;
    private final HashDemodayEntryCodePort hashDemodayEntryCodePort;

    @Override
    public CreateDemodayEntryCodesInfo create(Long memberId, CreateDemodayEntryCodeCommand command) {

        DemodayPoll demodayPoll = loadDemodayPollPort.findById(command.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        if (!demodayPoll.canGenerateEtnryCode()) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_ENTRY_CODE_GENERATION_NOT_ALLOWED);
        }

        demodayPoll.validEntryCodeGenerationAvailable(Instant.now());

        demodayAdminAccessChecker.validateAdminAccess(memberId, demodayPoll.getGisuId());

        List<String> rawCodes = new ArrayList<>();
        List<DemodayEntryCode> entryCodes = new ArrayList<>();

        for (int i = 0; i < command.count(); i++) {
            String rawCode = generateDemodayEntryCodePort.generate();
            String hashCode = hashDemodayEntryCodePort.hash(rawCode);

            rawCodes.add(rawCode);
            entryCodes.add(DemodayEntryCode.create(command.pollId(), hashCode));
        }

        saveDemodayEntryCodePort.saveAll(entryCodes);

        return CreateDemodayEntryCodesInfo.from(rawCodes);
    }
}
