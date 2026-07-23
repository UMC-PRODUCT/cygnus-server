package com.umc.product.community.application.service.command;

import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.THREAD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadSettingCommandService")
class CommunityThreadSettingCommandServiceTest {

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadMemberPort;
    @Mock
    SaveCommunityThreadMemberPort saveMemberPort;

    CommunityThreadSettingCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadSettingCommandService(
            loadThreadPort,
            loadMemberPort,
            saveMemberPort,
            new CommunityThreadProperties(100)
        );
    }

    @Test
    @DisplayName("ACTIVE 멤버는 스레드 잠금 뒤 자신의 pin을 설정한다")
    void pin_activeSelfAfterThreadLock() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(member));
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(2L);
        given(saveMemberPort.save(member)).willReturn(member);

        // when
        CommunityThreadLifecycleInfo result = sut.pin(new ThreadActorCommand(THREAD_ID, 20L));

        // then
        assertThat(result.pinned()).isTrue();
        InOrder order = Mockito.inOrder(loadThreadPort, loadMemberPort, saveMemberPort);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadMemberPort).findByThreadIdAndMemberId(THREAD_ID, 20L);
        order.verify(saveMemberPort).save(member);
    }

    @Test
    @DisplayName("이미 pin된 멤버의 동일 요청은 저장을 반복하지 않는다")
    void pin_isIdempotent() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        member.pin();
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(member));
        given(loadMemberPort.countActiveByThreadId(THREAD_ID)).willReturn(2L);

        // when
        CommunityThreadLifecycleInfo result = sut.pin(new ThreadActorCommand(THREAD_ID, 20L));

        // then
        assertThat(result.pinned()).isTrue();
        then(saveMemberPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("LEFT 멤버는 자신의 mute 설정도 변경할 수 없다")
    void mute_leftMemberIsDenied() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember leftMember = CommunityThreadLifecycleTestFixtures.member(
            20L,
            CommunityThreadMemberRole.MEMBER,
            CommunityThreadMemberState.LEFT
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(leftMember));

        // when & then
        assertThatThrownBy(() -> sut.mute(new ThreadActorCommand(THREAD_ID, 20L)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(saveMemberPort).shouldHaveNoInteractions();
    }
}
