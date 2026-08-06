package com.umc.product.community.application.service.command;

import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.NOW;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.OWNER_ID;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.THREAD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.LeaveChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.LeaveChatRoomCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ChangeCommunityThreadMemberRoleCommand;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadInvitationInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.InviteCommunityThreadMembersCommand;
import com.umc.product.community.application.port.in.command.thread.dto.KickCommunityThreadMemberCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.event.CommunityThreadMemberKickedEvent;
import com.umc.product.community.domain.event.CommunityThreadMemberLeftEvent;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadMembershipCommandService")
class CommunityThreadMembershipCommandServiceTest {

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadMemberPort;
    @Mock
    SaveCommunityThreadMemberPort saveMemberPort;
    @Mock
    CommunityThreadInviteManager inviteManager;
    @Mock
    LeaveChatRoomUseCase leaveChatRoomUseCase;
    @Mock
    DomainEventPublisher eventPublisher;

    CommunityThreadMembershipCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadMembershipCommandService(
            loadThreadPort,
            loadMemberPort,
            saveMemberPort,
            inviteManager,
            leaveChatRoomUseCase,
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("OWNER 초대는 스레드 잠금과 actor 검증 뒤에만 Chat 참여 흐름을 시작한다")
    void invite_locksAndAuthorizesBeforeInviteManager() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        CommunityThreadMember invited = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));
        given(inviteManager.invite(thread, List.of(20L), NOW)).willReturn(List.of(invited));
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(2L);

        // when
        CommunityThreadInvitationInfo result = sut.invite(
            new InviteCommunityThreadMembersCommand(THREAD_ID, OWNER_ID, List.of(20L))
        );

        // then
        assertThat(result.invitedMembers()).hasSize(1);
        assertThat(result.memberCount()).isEqualTo(2L);
        InOrder order = Mockito.inOrder(loadThreadPort, loadMemberPort, inviteManager, eventPublisher);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadMemberPort).findByThreadIdAndMemberId(THREAD_ID, OWNER_ID);
        order.verify(inviteManager).invite(thread, List.of(20L), NOW);
        order.verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("일반 MEMBER는 초대할 수 없다")
    void invite_memberIsDenied() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> sut.invite(
            new InviteCommunityThreadMembersCommand(THREAD_ID, 20L, List.of(30L))
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(inviteManager).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("강퇴는 Community 잠금과 권한 검증 뒤 Chat을 먼저 퇴장시키고 상태를 KICKED로 저장한다")
    void kick_callsChatOnlyAfterCommunityLockAndPermission() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        CommunityThreadMember target = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(target));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(target, owner));
        given(saveMemberPort.save(target)).willReturn(target);

        // when
        CommunityThreadMemberLifecycleInfo result = sut.kick(
            new KickCommunityThreadMemberCommand(THREAD_ID, OWNER_ID, 20L)
        );

        // then
        assertThat(result.state()).isEqualTo(CommunityThreadMemberState.KICKED);
        assertThat(result.memberCount()).isEqualTo(1L);
        InOrder order = Mockito.inOrder(
            loadThreadPort,
            loadMemberPort,
            leaveChatRoomUseCase,
            saveMemberPort,
            eventPublisher
        );
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadMemberPort).findByThreadIdAndMemberId(THREAD_ID, OWNER_ID);
        order.verify(loadMemberPort).findByThreadIdAndMemberId(THREAD_ID, 20L);
        order.verify(loadMemberPort).listByThreadId(THREAD_ID);
        order.verify(leaveChatRoomUseCase).leaveChatRoom(LeaveChatRoomCommand.of(100L, 20L));
        order.verify(saveMemberPort).save(target);
        order.verify(eventPublisher).publish(any(CommunityThreadMemberKickedEvent.class));
    }

    @Test
    @DisplayName("OWNER는 강퇴할 수 없고 Chat을 호출하지 않는다")
    void kick_ownerIsRejectedBeforeChat() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        CommunityThreadMember admin = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.ADMIN
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(admin));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> sut.kick(
            new KickCommunityThreadMemberCommand(THREAD_ID, 20L, OWNER_ID)
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_OWNER_CANNOT_BE_KICKED);
        then(leaveChatRoomUseCase).shouldHaveNoInteractions();
        then(saveMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("OWNER는 소유권 이전 전 탈퇴할 수 없다")
    void leave_ownerIsRejectedBeforeChat() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));

        // when & then
        assertThatThrownBy(() -> sut.leave(new ThreadActorCommand(THREAD_ID, OWNER_ID)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_OWNER_CANNOT_LEAVE);
        then(leaveChatRoomUseCase).shouldHaveNoInteractions();
        then(saveMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("탈퇴 이벤트는 상태 전환 전 membership joinedAt을 epoch snapshot으로 발행한다")
    void leave_publishesMembershipEpochBeforeTransition() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(member));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(owner, member));
        given(saveMemberPort.save(member)).willReturn(member);
        ArgumentCaptor<CommunityThreadMemberLeftEvent> eventCaptor =
            ArgumentCaptor.forClass(CommunityThreadMemberLeftEvent.class);

        // when
        sut.leave(new ThreadActorCommand(THREAD_ID, 20L));

        // then
        then(eventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().membershipJoinedAt()).isEqualTo(NOW);
        assertThat(eventCaptor.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("OWNER 지정은 대상에게 OWNER를 주고 기존 OWNER를 ADMIN으로 원자적으로 저장한다")
    void changeRole_ownerTransfersOwnershipAtomically() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        CommunityThreadMember target = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(target));
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(2L);

        // when
        CommunityThreadMemberLifecycleInfo result = sut.changeRole(
            new ChangeCommunityThreadMemberRoleCommand(
                THREAD_ID,
                OWNER_ID,
                20L,
                CommunityThreadMemberRole.OWNER
            )
        );

        // then
        assertThat(result.role()).isEqualTo(CommunityThreadMemberRole.OWNER);
        assertThat(target.getRole()).isEqualTo(CommunityThreadMemberRole.OWNER);
        assertThat(owner.getRole()).isEqualTo(CommunityThreadMemberRole.ADMIN);
        then(saveMemberPort).should().transferOwnership(THREAD_ID, OWNER_ID, 20L);
        then(eventPublisher).shouldHaveNoInteractions();
        then(leaveChatRoomUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ADMIN은 역할을 변경할 수 없다")
    void changeRole_adminIsDenied() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember admin = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.ADMIN
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> sut.changeRole(
            new ChangeCommunityThreadMemberRoleCommand(
                THREAD_ID,
                20L,
                30L,
                CommunityThreadMemberRole.MEMBER
            )
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_OWNER_REQUIRED);
        then(saveMemberPort).shouldHaveNoInteractions();
    }
}
