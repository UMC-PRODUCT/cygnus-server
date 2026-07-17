package com.umc.product.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.authentication.application.port.in.command.OAuthAuthenticationUseCase;
import com.umc.product.authentication.application.port.in.query.GetMemberOAuthUseCase;
import com.umc.product.authorization.application.port.in.command.EvictAuthoritySnapshotCacheUseCase;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.member.application.port.in.command.dto.UpdateMemberCommand;
import com.umc.product.member.application.port.out.LoadMemberPort;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.notification.application.port.in.SendWebhookAlarmUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;
import com.umc.product.term.application.port.in.command.ManageTermAgreementUseCase;

@ExtendWith(MockitoExtension.class)
class MemberFileUsageSynchronizationTest {

    @Mock
    LoadMemberPort loadMemberPort;
    @Mock
    SaveMemberPort saveMemberPort;
    @Mock
    MemberRegistrationValidator registrationValidator;
    @Mock
    OAuthAuthenticationUseCase oAuthAuthenticationUseCase;
    @Mock
    GetMemberOAuthUseCase getMemberOAuthUseCase;
    @Mock
    ManageTermAgreementUseCase manageTermAgreementUseCase;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    ManageFileUsageUseCase manageFileUsageUseCase;
    @Mock
    DomainEventPublisher eventPublisher;
    @Mock
    SendWebhookAlarmUseCase sendWebhookAlarmUseCase;
    @Mock
    EvictAuthoritySnapshotCacheUseCase evictAuthoritySnapshotCacheUseCase;

    @Test
    void 프로필_수정은_저장된_snapshot을_정확한_owner에_동기화한다() {
        MemberService service = new MemberService(
            loadMemberPort,
            saveMemberPort,
            registrationValidator,
            oAuthAuthenticationUseCase,
            getMemberOAuthUseCase,
            manageTermAgreementUseCase,
            getSchoolUseCase,
            manageFileUsageUseCase,
            eventPublisher,
            sendWebhookAlarmUseCase,
            evictAuthoritySnapshotCacheUseCase
        );
        Member member = member(10L, "old-file");
        given(loadMemberPort.findById(10L)).willReturn(Optional.of(member));
        given(saveMemberPort.save(member)).willReturn(member);

        service.updateMember(UpdateMemberCommand.forProfileUpdate(10L, 99L, "new-file"));

        ArgumentCaptor<ReplaceFileUsagesCommand> usageCaptor =
            ArgumentCaptor.forClass(ReplaceFileUsagesCommand.class);
        then(manageFileUsageUseCase).should().replaceUsages(usageCaptor.capture());
        ReplaceFileUsagesCommand usage = usageCaptor.getValue();
        assertThat(usage.owner()).isEqualTo(
            FileUsageCoordinate.of("member", "10", "profile-image")
        );
        assertThat(usage.fileIds()).containsExactly("new-file");
        assertThat(usage.requesterMemberId()).isEqualTo(99L);

        InOrder order = inOrder(saveMemberPort, manageFileUsageUseCase);
        order.verify(saveMemberPort).save(member);
        order.verify(manageFileUsageUseCase).replaceUsages(any());
    }

    @Test
    void null_프로필_이미지는_기존_값을_유지하고_snapshot을_재동기화한다() {
        MemberService service = new MemberService(
            loadMemberPort,
            saveMemberPort,
            registrationValidator,
            oAuthAuthenticationUseCase,
            getMemberOAuthUseCase,
            manageTermAgreementUseCase,
            getSchoolUseCase,
            manageFileUsageUseCase,
            eventPublisher,
            sendWebhookAlarmUseCase,
            evictAuthoritySnapshotCacheUseCase
        );
        Member member = member(10L, "old-file");
        given(loadMemberPort.findById(10L)).willReturn(Optional.of(member));
        given(saveMemberPort.save(member)).willReturn(member);

        service.updateMember(UpdateMemberCommand.forProfileUpdate(10L, 10L, null));

        assertThat(member.getProfileImageId()).isEqualTo("old-file");
        then(registrationValidator).shouldHaveNoInteractions();
        then(manageFileUsageUseCase).should().replaceUsages(new ReplaceFileUsagesCommand(
            FileUsageCoordinate.of("member", "10", "profile-image"),
            Set.of("old-file"),
            10L
        ));
    }

    @Test
    void OAuth_회원가입은_프로필_이미지를_명시적으로_거부한다() {
        MemberRegistrationValidator validator = new MemberRegistrationValidator(
            null,
            null,
            null
        );

        assertThatThrownBy(() -> validator.validateProfileImageNotProvided("oauth-file"))
            .isInstanceOf(MemberDomainException.class)
            .extracting("baseCode")
            .isEqualTo(MemberErrorCode.PROFILE_IMAGE_NOT_ALLOWED_DURING_REGISTRATION);
    }

    private Member member(Long id, String profileImageId) {
        Member member = Member.create("홍길동", "길동", "member-" + id + "@example.com", 1L, profileImageId);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
