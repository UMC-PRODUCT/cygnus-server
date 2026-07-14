package com.umc.product.member.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.application.port.in.command.RecordAuditLogUseCase;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.member.application.port.in.command.ManageMemberUseCase;
import com.umc.product.member.application.port.in.command.RegisterOAuthMemberUseCase;
import com.umc.product.member.application.port.in.command.dto.DeleteMemberCommand;
import com.umc.product.member.application.port.in.command.dto.OAuthRegisterMemberCommand;
import com.umc.product.member.application.port.in.command.dto.UpdateMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.notification.application.port.in.dto.SendWebhookAlarmCommand;
import com.umc.product.notification.domain.WebhookPlatform;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolNameInfo;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService implements ManageMemberUseCase, RegisterOAuthMemberUseCase {

    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;

    private final MemberRegistrationValidator registrationValidator;

    private final OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    private final GetMemberOAuthUseCase getMemberOAuthUseCase;
    private final ManageTermAgreementUseCase manageTermAgreementUseCase;
    private final GetSchoolUseCase getSchoolUseCase;

    private final RecordAuditLogUseCase recordAuditLogUseCase;
    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Override
    @Transactional
    public Long register(OAuthRegisterMemberCommand command) {
        registrationValidator.validateSchoolExists(command.schoolId());
        registrationValidator.validateProfileImageExists(command.profileImageId());
        registrationValidator.validateMandatoryTermsAgreed(command.termConsents());

        Member savedMember = saveMemberPort.save(command.toEntity());

        // OAuth 계정 정보 저장
        oAuthAuthenticationUseCase.linkOAuth(
            LinkOAuthCommand.builder()
                .memberId(savedMember.getId())
                .provider(command.provider())
                .providerId(command.providerId())
                .appleRefreshToken(command.appleRefreshToken())
                .appleClientId(command.appleClientId())
                .build()
        );

        // 약관 동의 정보 저장
        command.termConsents().forEach(termConsent ->
            manageTermAgreementUseCase.createTermConsent(
                termConsent.toCommand(savedMember.getId())
            )
        );

        SchoolDetailInfo school = getSchoolUseCase.getSchoolDetail(command.schoolId());
        MemberAuditEventFactory.MemberSnapshot memberSnapshot =
            MemberAuditEventFactory.snapshot(savedMember, school.schoolName());
        String logDescription = school.schoolName()
            + "소속 " + command.nickname() + "/" + command.name()
            + " 님이 회원 가입하셨습니다.";

        sendWebhookAlarmUseCase.sendBuffered(
            SendWebhookAlarmCommand.builder()
                .platforms(List.of(WebhookPlatform.TELEGRAM))
                .title("새로운 회원이 가입하였습니다.")
                .content(logDescription)
                .build()
        );

        recordAuditLogUseCase.record(MemberAuditEventFactory.registered(memberSnapshot));

        return savedMember.getId();
    }

    @Override
    @Transactional
    public List<Long> batchRegister(List<OAuthRegisterMemberCommand> commands) {
        commands.forEach(command -> {
            registrationValidator.validateSchoolExists(command.schoolId());
            registrationValidator.validateProfileImageExists(command.profileImageId());
            registrationValidator.validateMandatoryTermsAgreed(command.termConsents());
        });

        List<Member> savedMembers = saveMemberPort.saveAll(commands.stream()
            .map(OAuthRegisterMemberCommand::toEntity)
            .toList()
        );

        // OAuth 계정 정보 벌크 저장
        List<LinkOAuthCommand> oAuthCommands = new ArrayList<>();
        for (int i = 0; i < savedMembers.size(); i++) {
            Member savedMember = savedMembers.get(i);
            OAuthRegisterMemberCommand command = commands.get(i);

            oAuthCommands.add(
                LinkOAuthCommand.builder()
                    .memberId(savedMember.getId())
                    .provider(command.provider())
                    .providerId(command.providerId())
                    .build()
            );

            // 약관 동의 정보 저장
            command.termConsents().forEach(termConsent ->
                manageTermAgreementUseCase.createTermConsent(
                    termConsent.toCommand(savedMember.getId())
                )
            );
        }
        oAuthAuthenticationUseCase.linkOAuthBulk(oAuthCommands);

        Set<Long> schoolIds = commands.stream()
            .map(OAuthRegisterMemberCommand::schoolId)
            .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> schoolNames = getSchoolUseCase.batchGetNamesByIds(schoolIds).stream()
            .collect(java.util.stream.Collectors.toMap(
                SchoolNameInfo::schoolId,
                SchoolNameInfo::schoolName
            ));
        for (int index = 0; index < savedMembers.size(); index++) {
            Member savedMember = savedMembers.get(index);
            String schoolName = schoolNames.get(commands.get(index).schoolId());
            recordAuditLogUseCase.record(MemberAuditEventFactory.registered(
                MemberAuditEventFactory.snapshot(savedMember, schoolName)
            ));
        }

        return savedMembers.stream()
            .map(Member::getId)
            .toList();
    }

    @Override
    @Transactional
    public void updateMember(UpdateMemberCommand command) {
        Member member = findById(command.memberId());

        registrationValidator.validateProfileImageExists(command.newProfileImageId());

        member.updateProfile(command.newProfileImageId());
    }

    @Override
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        Long memberId = command.memberId();

        Member memberToDelete = loadMemberPort.findById(memberId)
            .orElseThrow(() -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));

        SchoolDetailInfo school = getSchoolUseCase.getSchoolDetail(memberToDelete.getSchoolId());
        MemberAuditEventFactory.MemberSnapshot memberSnapshot =
            MemberAuditEventFactory.snapshot(memberToDelete, school.schoolName());

        List<MemberOAuthInfo> linkedOAuths = getMemberOAuthUseCase.getOAuthList(memberId);

        // TODO: N+1 문제 해결 필요
        for (MemberOAuthInfo oAuthInfo : linkedOAuths) {
            oAuthAuthenticationUseCase.unlinkOAuth(
                UnlinkOAuthCommand.builder()
                    .memberId(memberId)
                    .memberOAuthId(oAuthInfo.memberOAuthId())
                    .isWithdrawal(true)
                    .googleAccessToken(command.googleAccessToken())
                    .kakaoAccessToken(command.kakaoAccessToken())
                    .build()
            );
        }

        saveMemberPort.delete(memberToDelete);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(memberId);

        recordAuditLogUseCase.record(MemberAuditEventFactory.withdrawn(memberSnapshot));
    }

    private Member findById(Long memberId) {
        return loadMemberPort.findById(memberId).orElseThrow(
            () -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
