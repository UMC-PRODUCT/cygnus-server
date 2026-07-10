package com.umc.product.certificate.application.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CertificateIssueContextResolver {

    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;

    CertificateIssueContext resolveSelf(IssueCertificateCommand command) {
        CertificateTemplate template = command.template();
        if (!template.supportsSelfIssue()) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_SELF_ISSUE_FORBIDDEN);
        }
        return resolve(
            template,
            command.requesterMemberId(),
            command.gisuId(),
            null,
            null,
            command.requesterMemberId()
        );
    }

    CertificateIssueContext resolveAdmin(AdminIssueCertificateCommand command) {
        CertificateTemplate template = command.template();
        return resolve(
            template,
            command.recipientMemberId(),
            command.gisuId(),
            resolveMeritTitle(command),
            command.meritDescription(),
            command.requesterMemberId()
        );
    }

    private CertificateIssueContext resolve(
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        MemberInfo member = getMemberUseCase.getById(recipientMemberId);
        GisuInfo gisu = getGisuUseCase.getById(gisuId);

        if (template.requiresGraduation()) {
            return resolveCompletion(member, gisu, template, issuedByMemberId);
        }
        return resolveMerit(member, gisu, template, meritTitle, meritDescription, issuedByMemberId);
    }

    private CertificateIssueContext resolveCompletion(
        MemberInfo member,
        GisuInfo gisu,
        CertificateTemplate template,
        Long issuedByMemberId
    ) {
        ChallengerInfo challenger = getChallengerUseCase.findByMemberIdAndGisuId(member.id(), gisu.gisuId())
            .orElseThrow(() -> new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET));
        if (challenger.challengerStatus() != ChallengerStatus.GRADUATED) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        return baseContext(template, member, gisu, null, null, issuedByMemberId);
    }

    private CertificateIssueContext resolveMerit(
        MemberInfo member,
        GisuInfo gisu,
        CertificateTemplate template,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        String normalizedMeritTitle = normalize(meritTitle);
        if (normalizedMeritTitle == null) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        return baseContext(
            template,
            member,
            gisu,
            normalizedMeritTitle,
            normalize(meritDescription),
            issuedByMemberId
        );
    }

    private CertificateIssueContext baseContext(
        CertificateTemplate template,
        MemberInfo member,
        GisuInfo gisu,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        return new CertificateIssueContext(
            template,
            template.issuer(),
            member.id(),
            member.name(),
            member.schoolName(),
            gisu.gisuId(),
            gisu.generation(),
            meritTitle,
            meritDescription,
            issuedByMemberId
        );
    }

    private String resolveMeritTitle(AdminIssueCertificateCommand command) {
        String overrideTitle = normalize(command.meritTitle());
        if (overrideTitle != null) {
            return overrideTitle;
        }
        return command.template().defaultMeritTitle();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
