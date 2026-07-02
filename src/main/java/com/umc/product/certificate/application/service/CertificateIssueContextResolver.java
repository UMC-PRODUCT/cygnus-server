package com.umc.product.certificate.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.umc.product.certificate.application.port.in.command.dto.AdminIssueCertificateCommand;
import com.umc.product.certificate.application.port.in.command.dto.IssueCertificateCommand;
import com.umc.product.certificate.domain.CertificateIssuer;
import com.umc.product.certificate.domain.CertificateTemplate;
import com.umc.product.certificate.domain.CertificateType;
import com.umc.product.certificate.domain.exception.CertificateErrorCode;
import com.umc.product.certificate.domain.exception.CertificateException;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.project.application.port.in.query.GetProjectMemberUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectMemberInfo;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class CertificateIssueContextResolver {

    private static final CertificateIssuer DEFAULT_ISSUER = CertificateIssuer.UNIVERSITY_MAKEUS_CHALLENGE;

    private final GetMemberUseCase getMemberUseCase;
    private final GetChallengerUseCase getChallengerUseCase;
    private final GetGisuUseCase getGisuUseCase;
    private final GetProjectMemberUseCase getProjectMemberUseCase;

    CertificateIssueContext resolveSelf(IssueCertificateCommand command) {
        if (command.type() == CertificateType.MERIT) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_SELF_ISSUE_FORBIDDEN);
        }
        return resolve(
            command.type(),
            null,
            command.requesterMemberId(),
            command.gisuId(),
            command.projectId(),
            DEFAULT_ISSUER,
            null,
            null,
            command.requesterMemberId()
        );
    }

    CertificateIssueContext resolveAdmin(AdminIssueCertificateCommand command) {
        CertificateTemplate template = command.template();
        return resolve(
            resolveType(command),
            template,
            command.recipientMemberId(),
            command.gisuId(),
            command.projectId(),
            resolveIssuer(command),
            resolveMeritTitle(command),
            command.meritDescription(),
            command.requesterMemberId()
        );
    }

    private CertificateIssueContext resolve(
        CertificateType type,
        CertificateTemplate template,
        Long recipientMemberId,
        Long gisuId,
        Long projectId,
        CertificateIssuer issuer,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        MemberInfo member = getMemberUseCase.getById(recipientMemberId);
        GisuInfo gisu = getGisuUseCase.getById(gisuId);

        return switch (type) {
            case COMPLETION -> resolveCompletion(member, gisu, template, issuer, issuedByMemberId);
            case PROJECT_PARTICIPATION -> resolveProjectParticipation(member, gisu, projectId, issuer, issuedByMemberId);
            case MERIT -> resolveMerit(member, gisu, template, issuer, meritTitle, meritDescription, issuedByMemberId);
        };
    }

    private CertificateIssueContext resolveCompletion(
        MemberInfo member,
        GisuInfo gisu,
        CertificateTemplate template,
        CertificateIssuer issuer,
        Long issuedByMemberId
    ) {
        ChallengerInfo challenger = getChallengerUseCase.findByMemberIdAndGisuId(member.id(), gisu.gisuId())
            .orElseThrow(() -> new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET));
        if (challenger.challengerStatus() != ChallengerStatus.GRADUATED) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        return baseContext(CertificateType.COMPLETION, template, issuer, member, gisu, null, null, null, null, issuedByMemberId);
    }

    private CertificateIssueContext resolveProjectParticipation(
        MemberInfo member,
        GisuInfo gisu,
        Long projectId,
        CertificateIssuer issuer,
        Long issuedByMemberId
    ) {
        if (projectId == null) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        ProjectMemberInfo projectMember = getProjectMemberUseCase.findByProjectIdAndMemberId(projectId, member.id())
            .orElseThrow(() -> new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET));
        if (!Objects.equals(projectMember.projectGisuId(), gisu.gisuId())) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        if (projectMember.status() != ProjectMemberStatus.COMPLETED) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        return baseContext(
            CertificateType.PROJECT_PARTICIPATION,
            null,
            issuer,
            member,
            gisu,
            projectMember.projectId(),
            projectMember.projectName(),
            null,
            null,
            issuedByMemberId
        );
    }

    private CertificateIssueContext resolveMerit(
        MemberInfo member,
        GisuInfo gisu,
        CertificateTemplate template,
        CertificateIssuer issuer,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        String normalizedMeritTitle = normalize(meritTitle);
        if (normalizedMeritTitle == null) {
            throw new CertificateException(CertificateErrorCode.CERTIFICATE_ELIGIBILITY_NOT_MET);
        }
        return baseContext(
            CertificateType.MERIT,
            template,
            issuer,
            member,
            gisu,
            null,
            null,
            normalizedMeritTitle,
            normalize(meritDescription),
            issuedByMemberId
        );
    }

    private CertificateIssueContext baseContext(
        CertificateType type,
        CertificateTemplate template,
        CertificateIssuer issuer,
        MemberInfo member,
        GisuInfo gisu,
        Long projectId,
        String projectName,
        String meritTitle,
        String meritDescription,
        Long issuedByMemberId
    ) {
        return new CertificateIssueContext(
            type,
            template,
            issuer,
            member.id(),
            member.name(),
            member.schoolName(),
            gisu.gisuId(),
            gisu.generation(),
            projectId,
            projectName,
            meritTitle,
            meritDescription,
            issuedByMemberId
        );
    }

    private CertificateType resolveType(AdminIssueCertificateCommand command) {
        return command.template() != null ? command.template().type() : command.type();
    }

    private CertificateIssuer resolveIssuer(AdminIssueCertificateCommand command) {
        if (command.template() != null) {
            return command.template().issuer();
        }
        return command.issuer() == null ? DEFAULT_ISSUER : command.issuer();
    }

    private String resolveMeritTitle(AdminIssueCertificateCommand command) {
        String overrideTitle = normalize(command.meritTitle());
        if (overrideTitle != null || command.template() == null) {
            return overrideTitle;
        }
        return command.template().defaultMeritTitle();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
