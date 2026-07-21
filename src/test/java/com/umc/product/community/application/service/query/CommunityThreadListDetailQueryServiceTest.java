package com.umc.product.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListFilter;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListCondition;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadListRows;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchChallengerInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadQueryService 목록/상세")
class CommunityThreadListDetailQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    CommunityThreadQueryPort threadQueryPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerUseCase getChallengerUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    SearchChallengerInvitationUseCase searchInvitationUseCase;

    CommunityThreadQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadQueryService(
            threadQueryPort,
            getMemberUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            searchInvitationUseCase,
            new CommunityThreadProperties(75)
        );
    }

    @Test
    @DisplayName("고정/일반 목록을 분리하고 일반 목록 기준 nextOffset과 total을 반환하며 발신자를 한 번에 조회한다")
    void listThreads_고정과_일반을_분리하고_발신자를_배치_조회한다() {
        // given
        CommunityThreadQueryRow pinned = activeRow(1L, true, 501L);
        CommunityThreadQueryRow first = activeRow(2L, false, 502L);
        CommunityThreadQueryRow second = activeRow(3L, false, 501L);
        given(threadQueryPort.searchThreads(org.mockito.ArgumentMatchers.any()))
            .willReturn(new CommunityThreadListRows(List.of(pinned), List.of(first, second), 5L));
        given(getMemberUseCase.findAllByIds(Set.of(501L, 502L)))
            .willReturn(Map.of(501L, member(501L, "가"), 502L, member(502L, "나")));

        // when
        ThreadListInfo result = sut.listThreads(new ListThreadsQuery(
            10L,
            ThreadListFilter.ALL,
            "  스레드  ",
            1,
            2
        ));

        // then
        assertThat(result.pinned()).extracting(info -> info.threadId()).containsExactly(1L);
        assertThat(result.threads()).extracting(info -> info.threadId()).containsExactly(2L, 3L);
        assertThat(result.threads()).extracting(info -> info.lastMessage().senderName())
            .containsExactly("나", "가");
        assertThat(result.nextOffset()).isEqualTo(3);
        assertThat(result.total()).isEqualTo(5L);
        assertThat(result.pinned().get(0).maxMembers()).isEqualTo(75);
        verify(getMemberUseCase).findAllByIds(Set.of(501L, 502L));

        ArgumentCaptor<CommunityThreadListCondition> captor =
            ArgumentCaptor.forClass(CommunityThreadListCondition.class);
        verify(threadQueryPort).searchThreads(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("스레드");
        assertThat(captor.getValue().category()).isNull();
        assertThat(captor.getValue().unreadOnly()).isFalse();
    }

    @Test
    @DisplayName("삭제된 스레드 상세는 THREAD_DELETED로 거절하고 발신자를 조회하지 않는다")
    void getThread_삭제된_스레드를_거절한다() {
        // given
        CommunityThreadQueryRow deleted = restrictedRow(
            CommunityThreadMemberState.ACTIVE,
            NOW.plusSeconds(1)
        );
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(deleted));

        // when & then
        assertThatThrownBy(() -> sut.getThread(new GetThreadDetailQuery(1L, 10L)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_DELETED);
        verifyNoInteractions(getMemberUseCase);
    }

    @Test
    @DisplayName("ACTIVE 멤버가 아닌 요청자의 상세 조회는 THREAD_ACCESS_DENIED로 거절한다")
    void getThread_비활성_멤버를_거절한다() {
        // given
        CommunityThreadQueryRow left = restrictedRow(CommunityThreadMemberState.LEFT, null);
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(left));

        // when & then
        assertThatThrownBy(() -> sut.getThread(new GetThreadDetailQuery(1L, 10L)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        verifyNoInteractions(getMemberUseCase);
    }

    @Test
    @DisplayName("삭제 직후 ACTIVE 멤버의 전체 상세 결과에 삭제 시각을 포함한다")
    void getMutationDetail_삭제된_스레드의_전체_상세를_반환한다() {
        // given
        Instant deletedAt = NOW.plusSeconds(60);
        Instant createdAt = NOW.minusSeconds(120);
        Instant updatedAt = NOW.minusSeconds(10);
        CommunityThreadQueryRow deleted = new CommunityThreadQueryRow(
            1L,
            "삭제된 스레드",
            "삭제 설명",
            CommunityThreadCategory.QNA,
            "❓",
            4L,
            3L,
            true,
            true,
            CommunityThreadMemberRole.ADMIN,
            CommunityThreadMemberState.ACTIVE,
            "마지막 메시지",
            501L,
            NOW.minusSeconds(20),
            10L,
            NOW.minusSeconds(5),
            deletedAt,
            createdAt,
            updatedAt
        );
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(deleted));
        given(getMemberUseCase.findAllByIds(Set.of(501L)))
            .willReturn(Map.of(501L, member(501L, "발신자")));

        // when
        var result = sut.getMutationDetail(new GetThreadDetailQuery(1L, 10L));

        // then
        assertThat(result.threadId()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("삭제된 스레드");
        assertThat(result.description()).isEqualTo("삭제 설명");
        assertThat(result.category()).isEqualTo(CommunityThreadCategory.QNA);
        assertThat(result.icon()).isEqualTo("❓");
        assertThat(result.memberCount()).isEqualTo(4L);
        assertThat(result.unreadCount()).isEqualTo(3L);
        assertThat(result.maxMembers()).isEqualTo(75);
        assertThat(result.isPinned()).isTrue();
        assertThat(result.isMuted()).isTrue();
        assertThat(result.myRole()).isEqualTo(CommunityThreadMemberRole.ADMIN);
        assertThat(result.lastMessage()).isNotNull();
        assertThat(result.lastMessage().preview()).isEqualTo("마지막 메시지");
        assertThat(result.lastMessage().senderName()).isEqualTo("발신자");
        assertThat(result.lastMessage().createdAt()).isEqualTo(NOW.minusSeconds(20));
        assertThat(result.createdBy()).isEqualTo(10L);
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
        assertThat(result.shareUrl()).isEqualTo("/api/v1/community/threads/1");
        assertThat(result.deletedAt()).isEqualTo(deletedAt);
        verify(getMemberUseCase).findAllByIds(Set.of(501L));
    }

    @Test
    @DisplayName("삭제 직후에도 ACTIVE 멤버가 아니면 mutation 결과를 거절한다")
    void getMutationDetail_비활성_멤버를_거절한다() {
        // given
        CommunityThreadQueryRow deleted = restrictedRow(
            CommunityThreadMemberState.LEFT,
            NOW.plusSeconds(1)
        );
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(deleted));

        // when & then
        assertThatThrownBy(() -> sut.getMutationDetail(new GetThreadDetailQuery(1L, 10L)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        verifyNoInteractions(getMemberUseCase);
    }

    private CommunityThreadQueryRow activeRow(Long threadId, boolean pinned, Long senderId) {
        return new CommunityThreadQueryRow(
            threadId,
            "스레드 " + threadId,
            "설명",
            CommunityThreadCategory.STUDY,
            "📚",
            3L,
            2L,
            pinned,
            false,
            CommunityThreadMemberRole.MEMBER,
            CommunityThreadMemberState.ACTIVE,
            senderId == null ? null : "마지막 메시지",
            senderId,
            senderId == null ? null : NOW,
            10L,
            NOW,
            null,
            NOW,
            NOW
        );
    }

    private CommunityThreadQueryRow restrictedRow(CommunityThreadMemberState state, Instant deletedAt) {
        return new CommunityThreadQueryRow(
            1L, "스레드 1", "설명", CommunityThreadCategory.STUDY, "📚",
            3L, 2L, false, false, CommunityThreadMemberRole.MEMBER, state,
            null, null, null, 10L, NOW, deletedAt, NOW, NOW
        );
    }

    private MemberInfo member(Long memberId, String name) {
        return MemberInfo.builder().id(memberId).name(name).build();
    }
}
