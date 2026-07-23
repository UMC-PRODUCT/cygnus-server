package com.umc.product.term.application.service.command;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.term.application.port.in.command.SubmitRequiredTermReconsentUseCase;
import com.umc.product.term.application.port.in.command.dto.SubmitRequiredTermReconsentCommand;
import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.application.port.out.SaveTermConsentLogPort;
import com.umc.product.term.application.port.out.SaveTermConsentPort;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.TermConsentLog;
import com.umc.product.term.domain.enums.TermConsentStatus;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RequiredTermReconsentCommandService implements SubmitRequiredTermReconsentUseCase {

    private final LoadTermPort loadTermPort;
    private final SaveTermConsentPort saveTermConsentPort;
    private final SaveTermConsentLogPort saveTermConsentLogPort;

    @Override
    public void submitRequiredTermReconsent(SubmitRequiredTermReconsentCommand command) {
        Term term = loadTermPort.findByIdWithSharedLock(command.termId())
            .orElseThrow(() -> new TermDomainException(TermErrorCode.TERMS_NOT_FOUND));
        if (!term.isActive() || !term.isRequired()) {
            throw new TermDomainException(TermErrorCode.INVALID_RECONSENT_TERM);
        }

        TermConsent termConsent = TermConsent.builder()
            .memberId(command.memberId())
            .termId(term.getId())
            .termType(term.getType())
            .agreedAt(Instant.now())
            .build();
        if (saveTermConsentPort.saveIfAbsent(termConsent)) {
            saveTermConsentLogPort.save(
                TermConsentLog.builder()
                    .memberId(command.memberId())
                    .termId(term.getId())
                    .termType(term.getType())
                    .status(TermConsentStatus.AGREED)
                    .build()
            );
        }
    }
}
