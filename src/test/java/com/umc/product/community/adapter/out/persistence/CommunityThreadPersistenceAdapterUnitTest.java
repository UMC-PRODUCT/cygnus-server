package com.umc.product.community.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadPersistenceAdapter 단위 경계")
class CommunityThreadPersistenceAdapterUnitTest {

    @Mock
    CommunityThreadRepository threadRepository;
    @Mock
    CommunityThreadMemberRepository memberRepository;
    @Mock
    CommunityThreadQueryRepository queryRepository;

    @InjectMocks
    CommunityThreadPersistenceAdapter sut;

    @Test
    @DisplayName("검색과 초대 차단 ID 조회를 Query repository에 그대로 위임한다")
    void delegatesQueryOperations() {
        CommunityThreadListCondition condition = mock(CommunityThreadListCondition.class);
        CommunityThreadListRows rows = new CommunityThreadListRows(List.of(), List.of(), 0L);
        given(queryRepository.searchThreads(condition)).willReturn(rows);
        given(queryRepository.listInvitationBlockedMemberIds(1L)).willReturn(List.of(10L, 20L));

        assertThat(sut.searchThreads(condition)).isSameAs(rows);
        assertThat(sut.listInvitationBlockedMemberIds(1L)).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("소유권 이전에서 기존 OWNER가 정확히 한 명 갱신되지 않으면 승격하지 않는다")
    void transferOwnership_requiresExactlyOneDemotedOwner() {
        given(memberRepository.demoteOwner(1L, 10L)).willReturn(0);

        assertThatThrownBy(() -> sut.transferOwnership(1L, 10L, 20L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ownership transfer must demote exactly one owner");
        then(memberRepository).should().demoteOwner(1L, 10L);
        then(memberRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("소유권 이전에서 새 OWNER가 정확히 한 명 갱신되지 않으면 실패한다")
    void transferOwnership_requiresExactlyOnePromotedOwner() {
        given(memberRepository.demoteOwner(1L, 10L)).willReturn(1);
        given(memberRepository.promoteOwner(1L, 20L)).willReturn(0);

        assertThatThrownBy(() -> sut.transferOwnership(1L, 10L, 20L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("ownership transfer must promote exactly one owner");
    }
}
