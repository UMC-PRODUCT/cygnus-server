package com.umc.product.member.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditLogEvent;
import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.command.dto.LinkOAuthCommand;
import com.umc.product.authentication.application.port.in.command.dto.UnlinkOAuthCommand;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authentication.application.port.in.query.dto.MemberOAuthInfo;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.global.exception.constant.Domain;
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
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
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
    private final ManageFileUsageUseCase manageFileUsageUseCase;

    private final DomainEventPublisher eventPublisher;
    private final SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    private final EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Override
    @Transactional
    public Long register(OAuthRegisterMemberCommand command) {
        registrationValidator.validateSchoolExists(command.schoolId());
        registrationValidator.validateProfileImageNotProvided(command.profileImageId());
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

        String logDescription = getSchoolUseCase.getSchoolDetail(command.schoolId()).schoolName()
            + "소속 " + command.nickname() + "/" + command.name() +
            " 님이 회원 가입하셨습니다.";

        sendWebhookAlarmUseCase.sendBuffered(
            SendWebhookAlarmCommand.builder()
                .platforms(List.of(WebhookPlatform.TELEGRAM))
                .title("새로운 회원이 가입하였습니다.")
                .content(logDescription)
                .build()
        );

        eventPublisher.publish(
            AuditLogEvent.builder()
                .domain(Domain.MEMBER)
                .action(AuditAction.REGISTER)
                .targetType("Member")
                .targetId(String.valueOf(savedMember.getId()))
                .description(logDescription)
                .build()
        );

        return savedMember.getId();
    }

    @Override
    @Transactional
    public List<Long> batchRegister(List<OAuthRegisterMemberCommand> commands) {
        commands.forEach(command -> {
            registrationValidator.validateSchoolExists(command.schoolId());
            registrationValidator.validateProfileImageNotProvided(command.profileImageId());
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

        return savedMembers.stream()
            .map(Member::getId)
            .toList();
    }

    @Override
    @Transactional
    public void updateMember(UpdateMemberCommand command) {
        Member member = findById(command.memberId());
        Long requesterMemberId = Objects.requireNonNull(
            command.requesterMemberId(),
            "프로필 수정 요청 회원 ID는 필수입니다."
        );

        if (command.newProfileImageId() != null
            && !command.newProfileImageId().isBlank()
            && !Objects.equals(member.getProfileImageId(), command.newProfileImageId())) {
            registrationValidator.validateProfileImageUsable(command.newProfileImageId(), requesterMemberId);
        }

        member.updateProfile(command.newProfileImageId());
        Member savedMember = saveMemberPort.save(member);
        replaceMemberProfileUsage(savedMember, requesterMemberId);
    }

    @Override
    @Transactional
    public void deleteMember(DeleteMemberCommand command) {
        Long memberId = command.memberId();

        Member memberToDelete = loadMemberPort.findById(memberId)
            .orElseThrow(() -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));

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

        manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(List.of(memberProfileCoordinate(memberId))));
        saveMemberPort.delete(memberToDelete);
        evictAuthoritySnapshotCacheUseCase.evictByMemberId(memberId);

        eventPublisher.publish(
            AuditLogEvent.builder()
                .domain(Domain.MEMBER)
                .action(AuditAction.WITHDRAW)
                .targetType("Member")
                .targetId(String.valueOf(memberId))
                .description(
                    getSchoolUseCase.getSchoolDetail(memberToDelete.getSchoolId()).schoolName()
                        + "소속 " + memberToDelete.getNickname() + "/" + memberToDelete.getName() +
                        " 님이 회원 탈퇴하셨습니다.")
                .build()
        );
    }

    private Member findById(Long memberId) {
        return loadMemberPort.findById(memberId).orElseThrow(
            () -> new MemberDomainException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private void replaceMemberProfileUsage(Member member, Long requesterMemberId) {
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            memberProfileCoordinate(member.getId()),
            member.getProfileImageId() == null ? Set.of() : Set.of(member.getProfileImageId()),
            requesterMemberId
        ));
    }

    private FileUsageCoordinate memberProfileCoordinate(Long memberId) {
        return FileUsageCoordinate.of("member", String.valueOf(memberId), "profile-image");
    }
}
