package com.umc.product.member.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.umc.product.member.application.port.in.command.dto.TermConsents;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.term.application.port.in.query.GetTermUseCase;
import com.umc.product.term.domain.exception.TermDomainException;
import com.umc.product.term.domain.exception.TermErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberRegistrationValidator {

    private final GetTermUseCase getTermUseCase;
    private final GetSchoolUseCase getSchoolUseCase;
    private final GetFileUseCase getFileUseCase;

    protected void validateProfileImageNotProvided(String profileImageId) {
        if (profileImageId != null) {
            throw new MemberDomainException(MemberErrorCode.PROFILE_IMAGE_NOT_ALLOWED_DURING_REGISTRATION);
        }
    }

    protected void validateProfileImageUsable(String profileImageId, Long requesterMemberId) {
        if (profileImageId != null) {
            getFileUseCase.batchGetUsableByIds(List.of(profileImageId), requesterMemberId);
        }
    }

    protected void validateSchoolExists(Long schoolId) {
        getSchoolUseCase.getSchoolDetail(schoolId);
    }

    protected void validateMandatoryTermsAgreed(List<TermConsents> termConsents) {
        Set<Long> requiredTermIds = getTermUseCase.getRequiredTermIds();

        Set<Long> agreedTermIds = termConsents.stream()
            .filter(TermConsents::isAgreed)
            .map(TermConsents::termId)
            .collect(Collectors.toSet());

        if (!agreedTermIds.containsAll(requiredTermIds)) {
            throw new TermDomainException(TermErrorCode.MANDATORY_TERMS_NOT_AGREED);
        }
    }
}
