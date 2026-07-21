package com.umc.product.community.application.service.command;

import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.NOW;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.THREAD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.SearchChallengerInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.ChallengerInvitationInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadInviteManager")
class CommunityThreadInviteManagerTest {

    @Mock
    SearchChallengerInvitationUseCase searchInvitationUseCase;
    @Mock
    LoadCommunityThreadMemberPort loadMemberPort;
    @Mock
    SaveCommunityThreadMemberPort saveMemberPort;
    @Mock
    JoinChatRoomUseCase joinChatRoomUseCase;

    CommunityThreadInviteManager sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadInviteManager(
            searchInvitationUseCase,
            loadMemberPort,
            saveMemberPort,
            joinChatRoomUseCase,
            new CommunityThreadProperties(100)
        );
    }

    @Test
    @DisplayName("기존 메시지 thread 신규 초대는 현재 메시지까지 읽고 참여한다")
    void invite_freshMemberStartsCaughtUpAtCurrentLastMessage() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        thread.updateLastMessage(900L, "기존 메시지", 10L, NOW);
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(1L);
        given(loadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(20L, 30L)
        )).willReturn(List.of());
        given(searchInvitationUseCase.batchGetEligibleChallengers(
            Set.of(20L, 30L)
        )).willReturn(Map.of(
            20L, invitation(20L),
            30L, invitation(30L)
        ));
        given(saveMemberPort.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<CommunityThreadMember> invited = sut.invite(
            thread,
            List.of(30L, 20L),
            NOW
        );

        // then
        assertThat(invited).extracting(CommunityThreadMember::getMemberId).containsExactly(20L, 30L);
        assertThat(invited).extracting(CommunityThreadMember::getUnreadCount).containsOnly(0L);
        ArgumentCaptor<JoinChatRoomCommand> commandCaptor = ArgumentCaptor.forClass(JoinChatRoomCommand.class);
        then(joinChatRoomUseCase).should(Mockito.times(2)).joinChatRoom(commandCaptor.capture());
        assertThat(commandCaptor.getAllValues()).containsExactly(
            new JoinChatRoomCommand(100L, 20L, 900L),
            new JoinChatRoomCommand(100L, 30L, 900L)
        );
        InOrder order = Mockito.inOrder(
            searchInvitationUseCase,
            joinChatRoomUseCase,
            saveMemberPort
        );
        order.verify(searchInvitationUseCase).batchGetEligibleChallengers(
            Set.of(20L, 30L)
        );
        order.verify(joinChatRoomUseCase, Mockito.times(2)).joinChatRoom(any());
        order.verify(saveMemberPort).saveAll(any());
    }

    @Test
    @DisplayName("LEFT 재초대는 현재 메시지까지 읽고 기존 unread를 초기화한다")
    void invite_leftMemberRejoinsCaughtUp() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        thread.updateLastMessage(900L, "LEFT 기간 메시지", 10L, NOW);
        CommunityThreadMember leftMember = CommunityThreadLifecycleTestFixtures.member(
            20L,
            CommunityThreadMemberRole.ADMIN,
            CommunityThreadMemberState.LEFT
        );
        leftMember.updateUnreadCount(7L);
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(1L);
        given(loadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(20L)
        )).willReturn(List.of(leftMember));
        given(searchInvitationUseCase.batchGetEligibleChallengers(
            Set.of(20L)
        )).willReturn(Map.of(20L, invitation(20L)));
        given(saveMemberPort.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<CommunityThreadMember> invited = sut.invite(
            thread,
            List.of(20L),
            NOW
        );

        // then
        assertThat(invited).containsExactly(leftMember);
        assertThat(leftMember.getState()).isEqualTo(CommunityThreadMemberState.ACTIVE);
        assertThat(leftMember.getRole()).isEqualTo(CommunityThreadMemberRole.MEMBER);
        assertThat(leftMember.getUnreadCount()).isZero();
        InOrder order = Mockito.inOrder(joinChatRoomUseCase, saveMemberPort);
        order.verify(joinChatRoomUseCase).joinChatRoom(new JoinChatRoomCommand(100L, 20L, 900L));
        order.verify(saveMemberPort).saveAll(any());
    }

    @Test
    @DisplayName("KICKED 멤버는 재초대를 거절하고 Chat을 호출하지 않는다")
    void invite_kickedMemberIsRejected() {
        // given
        CommunityThreadMember kickedMember = CommunityThreadLifecycleTestFixtures.member(
            20L,
            CommunityThreadMemberRole.MEMBER,
            CommunityThreadMemberState.KICKED
        );
        given(loadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(20L)
        )).willReturn(List.of(kickedMember));

        // when & then
        assertThatThrownBy(() -> sut.invite(
            CommunityThreadLifecycleTestFixtures.thread(),
            List.of(20L),
            NOW
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_MEMBER_KICKED);
        then(searchInvitationUseCase).shouldHaveNoInteractions();
        then(joinChatRoomUseCase).shouldHaveNoInteractions();
        then(saveMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("ACTIVE 멤버 수와 초대 수가 설정 용량을 넘으면 Chat 호출 전에 거절한다")
    void invite_capacityExceededBeforeChat() {
        // given
        sut = new CommunityThreadInviteManager(
            searchInvitationUseCase,
            loadMemberPort,
            saveMemberPort,
            joinChatRoomUseCase,
            new CommunityThreadProperties(2)
        );
        given(loadMemberPort.listByThreadIdAndMemberIds(
            THREAD_ID,
            Set.of(20L)
        )).willReturn(List.of());
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(2L);

        // when & then
        assertThatThrownBy(() -> sut.invite(
            CommunityThreadLifecycleTestFixtures.thread(),
            List.of(20L),
            NOW
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        then(searchInvitationUseCase).shouldHaveNoInteractions();
        then(joinChatRoomUseCase).shouldHaveNoInteractions();
    }

    private ChallengerInvitationInfo invitation(Long memberId) {
        return new ChallengerInvitationInfo(memberId, memberId + 1_000L, "이름", null, 12L);
    }

}
