package com.umc.product.demoday.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;
import com.umc.product.demoday.application.port.in.query.dto.ListDemodayAdminVoteQuery;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantType;
import com.umc.product.demoday.application.port.out.DemodayVoteSearchCondition;
import com.umc.product.demoday.application.port.out.LoadDemodayBoothPort;
import com.umc.product.demoday.application.port.out.LoadDemodayEntryCodePort;
import com.umc.product.demoday.application.port.out.LoadDemodayPollPort;
import com.umc.product.demoday.application.port.out.SearchDemodayVotePort;
import com.umc.product.demoday.application.service.DemodayAdminAccessChecker;
import com.umc.product.demoday.domain.DemodayBooth;
import com.umc.product.demoday.domain.DemodayEntryCode;
import com.umc.product.demoday.domain.DemodayPoll;
import com.umc.product.demoday.domain.DemodayVote;
import com.umc.product.demoday.domain.enums.DemodayVoteStatus;
import com.umc.product.demoday.domain.exception.DemodayDomainException;
import com.umc.product.demoday.domain.exception.DemodayErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.project.application.port.in.query.GetProjectUseCase;
import com.umc.product.project.application.port.in.query.dto.ProjectInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemodayAdminVoteQueryService")
class DemodayAdminVoteQueryServiceTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long POLL_ID = 10L;
    private static final Long GISU_ID = 8L;
    private static final Instant OPENS_AT = Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant CLOSES_AT = Instant.parse("2026-08-15T12:00:00Z");

    @Mock private DemodayAdminAccessChecker adminAccessChecker;
    @Mock private LoadDemodayPollPort loadDemodayPollPort;
    @Mock private SearchDemodayVotePort searchDemodayVotePort;
    @Mock private LoadDemodayBoothPort loadDemodayBoothPort;
    @Mock private LoadDemodayEntryCodePort loadDemodayEntryCodePort;
    @Mock private GetMemberUseCase getMemberUseCase;
    @Mock private GetProjectUseCase getProjectUseCase;

    @InjectMocks private DemodayAdminVoteQueryService service;

    @Test
    @DisplayName("무효 표를 포함해 최신순으로 조회하고 외부인 번호를 Poll 전체 입장 순서로 표시한다")
    void listVotesWithStableGuestOrdinalAndCursor() {
        // given
        DemodayBooth projectBooth = projectBooth(20L, 11, 101L);
        DemodayBooth externalBooth = externalBooth(21L, 12, "외부 참가팀 A");
        DemodayEntryCode firstEntryCode = redeemedEntryCode(31L, "2026-08-15T08:00:00Z");
        DemodayEntryCode secondEntryCode = redeemedEntryCode(32L, "2026-08-15T08:01:00Z");

        DemodayVote memberVote = memberVote(30L, 100L, projectBooth, "2026-08-15T10:00:00Z");
        memberVote.revoke(Instant.parse("2026-08-15T10:10:00Z"));
        DemodayVote guestVote = guestVote(29L, secondEntryCode, externalBooth, "2026-08-15T09:59:00Z");
        DemodayVote lookaheadVote = memberVote(28L, 101L, projectBooth, "2026-08-15T09:58:00Z");

        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll()));
        given(searchDemodayVotePort.search(new DemodayVoteSearchCondition(
            POLL_ID, null, null, null, 3)))
            .willReturn(List.of(memberVote, guestVote, lookaheadVote));

        given(loadDemodayBoothPort.listByPollId(POLL_ID)).willReturn(List.of(projectBooth, externalBooth));
        given(getProjectUseCase.findAllByIds(Set.of(101L))).willReturn(Map.of(
            101L, ProjectInfo.builder().id(101L).name("잇픽").build()));
        given(getMemberUseCase.findAllNamesByIds(Set.of(100L))).willReturn(Map.of(100L, "이재원"));

        given(loadDemodayEntryCodePort.listRedeemedByPollId(POLL_ID))
            .willReturn(List.of(firstEntryCode, secondEntryCode));

        // when
        DemodayAdminVoteListInfo result = service.listVotes(
            new ListDemodayAdminVoteQuery(POLL_ID, REQUESTER_ID, null, 2, null, null));

        // then
        assertThat(result.nextCursor()).isEqualTo(guestVote.getId());
        assertThat(result.hasNext()).isTrue();
        assertThat(result.content())
            .extracting(
                DemodayAdminVoteInfo::voteId,
                info -> info.participant().type(),
                info -> info.participant().displayName(),
                info -> info.booth().boothCode(),
                info -> info.booth().displayName(),
                DemodayAdminVoteInfo::status)
            .containsExactly(
                tuple(30L, DemodayParticipantType.MEMBER, "이재원", 11, "잇픽", DemodayVoteStatus.REVOKED),
                tuple(29L, DemodayParticipantType.GUEST, "외부인 2번", 12, "외부 참가팀 A", DemodayVoteStatus.VALID));
        then(adminAccessChecker).should().validateAdminAccess(REQUESTER_ID, GISU_ID);
    }

    @Test
    @DisplayName("회원명과 부스 필터를 AND 조건으로 검색 포트에 전달한다")
    void combineParticipantNameAndBoothFilters() {
        // given
        Long boothId = 20L;
        Set<Long> candidateMemberIds = Set.of(100L, 101L);
        Set<Long> matchingMemberIds = Set.of(101L);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll()));
        given(searchDemodayVotePort.listMemberIds(POLL_ID, boothId)).willReturn(candidateMemberIds);
        given(getMemberUseCase.searchIdsByName(candidateMemberIds, "재원")).willReturn(matchingMemberIds);
        given(searchDemodayVotePort.search(new DemodayVoteSearchCondition(
            POLL_ID, 50L, boothId, matchingMemberIds, 51)))
            .willReturn(List.of());

        // when
        DemodayAdminVoteListInfo result = service.listVotes(
            new ListDemodayAdminVoteQuery(POLL_ID, REQUESTER_ID, 50L, 50, boothId, "  재원  ")
        );

        // then
        assertThat(result.content()).isEmpty();
        then(searchDemodayVotePort).should().search(new DemodayVoteSearchCondition(
            POLL_ID, 50L, boothId, matchingMemberIds, 51
        ));
    }

    @Test
    @DisplayName("회원명과 일치하는 투표자가 없으면 투표 행을 조회하지 않는다")
    void skipVoteSearchWhenNoMemberMatchesName() {
        // given
        Long boothId = 20L;
        Set<Long> candidateMemberIds = Set.of(100L);
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.of(poll()));
        given(searchDemodayVotePort.listMemberIds(POLL_ID, boothId)).willReturn(candidateMemberIds);
        given(getMemberUseCase.searchIdsByName(candidateMemberIds, "없는 이름")).willReturn(Set.of());

        // when
        DemodayAdminVoteListInfo result = service.listVotes(
            new ListDemodayAdminVoteQuery(POLL_ID, REQUESTER_ID, null, 50, boothId, "없는 이름"));

        // then
        assertThat(result).isEqualTo(DemodayAdminVoteListInfo.empty(POLL_ID));
        then(searchDemodayVotePort).should().listMemberIds(POLL_ID, boothId);
        then(searchDemodayVotePort).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Poll이 없으면 권한 검사나 투표 조회를 수행하지 않는다")
    void rejectWhenPollDoesNotExist() {
        // given
        given(loadDemodayPollPort.findById(POLL_ID)).willReturn(Optional.empty());
        ListDemodayAdminVoteQuery query =
            new ListDemodayAdminVoteQuery(POLL_ID, REQUESTER_ID, null, 50, null, null);

        // when & then
        assertThatThrownBy(() -> service.listVotes(query))
            .isInstanceOfSatisfying(
                DemodayDomainException.class,
                exception -> assertThat(exception.getBaseCode())
                    .isEqualTo(DemodayErrorCode.DEMODAY_POLL_NOT_FOUND));

        then(adminAccessChecker).shouldHaveNoInteractions();
        then(searchDemodayVotePort).shouldHaveNoInteractions();
    }

    private DemodayPoll poll() {
        DemodayPoll poll = DemodayPoll.create(GISU_ID, "8기 데모데이", OPENS_AT, CLOSES_AT);
        ReflectionTestUtils.setField(poll, "id", POLL_ID);
        return poll;
    }

    private DemodayBooth projectBooth(Long id, Integer boothCode, Long projectId) {
        DemodayBooth booth = DemodayBooth.forProject(POLL_ID, boothCode, projectId);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private DemodayBooth externalBooth(Long id, Integer boothCode, String displayName) {
        DemodayBooth booth = DemodayBooth.forExternal(POLL_ID, boothCode, displayName);
        ReflectionTestUtils.setField(booth, "id", id);
        return booth;
    }

    private DemodayEntryCode redeemedEntryCode(Long id, String redeemedAt) {
        DemodayEntryCode entryCode = DemodayEntryCode.create(POLL_ID, String.valueOf(id).repeat(32));
        ReflectionTestUtils.setField(entryCode, "id", id);
        entryCode.redeem(Instant.parse(redeemedAt));
        return entryCode;
    }

    private DemodayVote memberVote(Long id, Long memberId, DemodayBooth booth, String votedAt) {
        DemodayVote vote = DemodayVote.forMember(POLL_ID, memberId, booth);
        persistVote(vote, id, votedAt);
        return vote;
    }

    private DemodayVote guestVote(Long id, DemodayEntryCode entryCode, DemodayBooth booth, String votedAt) {
        DemodayVote vote = DemodayVote.forVisitor(POLL_ID, entryCode, booth);
        persistVote(vote, id, votedAt);
        return vote;
    }

    private void persistVote(DemodayVote vote, Long id, String votedAt) {
        ReflectionTestUtils.setField(vote, "id", id);
        ReflectionTestUtils.setField(vote, "createdAt", Instant.parse(votedAt));
    }
}
