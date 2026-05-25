package com.umc.product.term.application.service.query;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.term.application.port.in.query.GetRequiredTermConsentStatusUseCase;
import com.umc.product.term.application.port.in.query.dto.RequiredTermConsentStatusInfo;
import com.umc.product.term.application.port.out.LoadTermConsentPort;
import com.umc.product.term.application.port.out.LoadTermPort;
import com.umc.product.term.domain.Term;
import com.umc.product.term.domain.TermConsent;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequiredTermConsentStatusQueryService implements GetRequiredTermConsentStatusUseCase {

    private final LoadTermPort loadTermPort;
    private final LoadTermConsentPort loadTermConsentPort;

    @Override
    public RequiredTermConsentStatusInfo getRequiredTermConsentStatus(Long memberId) {
        if (memberId == null) {
            throw new TermDomainException(TermErrorCode.MEMBER_ID_REQUIRED);
        }

        List<Term> requiredTerms = loadTermPort.findAllActiveRequired();
        if (requiredTerms.isEmpty()) {
            return RequiredTermConsentStatusInfo.fromMissingTerms(List.of());
        }

        List<Long> requiredTermIds = requiredTerms.stream()
            .map(Term::getId)
            .toList();
        Set<Long> agreedTermIdSet = loadTermConsentPort.listByMemberIdAndTermIds(memberId, requiredTermIds)
            .stream()
            .map(TermConsent::getTermId)
            .collect(Collectors.toSet());
        List<Long> agreedRequiredTermIds = requiredTermIds.stream()
            .filter(agreedTermIdSet::contains)
            .toList();

        return RequiredTermConsentStatusInfo.fromRequiredTerms(requiredTerms, agreedRequiredTermIds);
    }
}
