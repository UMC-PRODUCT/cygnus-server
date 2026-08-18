package com.umc.product.demoday.application.service.command;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.demoday.application.port.in.command.CreateDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateStampCredentialCommand;
import com.umc.product.demoday.application.port.in.command.dto.StampCredentialInfo;
import com.umc.product.demoday.application.port.out.EncryptDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.GenerateDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.HashDemodayStampCredentialPort;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.config.DemodayQrProperties;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateDemodayStampCommandService implements CreateDemodayStampUseCase {

    private final DemodayAdminAccessChecker adminAccessChecker;
    private final LoadDemodayBoothPort loadDemodayBoothPort;
    private final LoadDemodayPollPort loadDemodayPollPort;
    private final EncryptDemodayStampCredentialPort encryptDemodayStampCredentialPort;
    private final GenerateDemodayStampCredentialPort generateDemodayStampCredentialPort;
    private final HashDemodayStampCredentialPort hashDemodayStampCredentialPort;

    private final Clock clock;
    private final DemodayQrProperties demodayQrProperties;

    @Override
    public StampCredentialInfo create(Long memberId, CreateStampCredentialCommand command) {
        DemodayBooth demodayBooth = loadDemodayBoothPort.findById(command.boothId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_NOT_FOUND));

        if (!demodayBooth.getPollId().equals(command.pollId())) {
            throw new DemodayDomainException(DemodayErrorCode.DEMODAY_BOOTH_NOT_FOUND);
        }

        DemodayPoll demodayPoll = loadDemodayPollPort.findById(command.pollId())
            .orElseThrow(() -> new DemodayDomainException(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        adminAccessChecker.validateAdminAccess(memberId, demodayPoll.getGisuId());

        String credential = generateDemodayStampCredentialPort.generate();
        String credentialHash = hashDemodayStampCredentialPort.hash(credential);
        String encryptedCredential = encryptDemodayStampCredentialPort.encrypt(credential);
        Instant generatedAt = clock.instant();

        demodayBooth.applyStampCredential(credentialHash, encryptedCredential, generatedAt);

        return new StampCredentialInfo(
            "%s/demoday/polls/%d/stamp#credential=%s".formatted(
                demodayQrProperties.baseUrl(),
                command.pollId(),
                credential
            ),
            generatedAt
        );
    }
}
