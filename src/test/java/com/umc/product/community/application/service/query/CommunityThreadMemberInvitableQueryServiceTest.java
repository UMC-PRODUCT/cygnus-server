package com.umc.product.community.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadMembersByIdsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ListThreadMembersQuery;
import com.umc.product.community.application.port.in.query.thread.dto.SearchThreadInvitableQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitablePageInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberPageInfo;
import com.umc.product.community.application.port.out.thread.CommunityThreadQueryPort;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadMemberRow;
import com.umc.product.community.application.port.out.thread.dto.CommunityThreadQueryRow;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.enums.CommunityThreadMemberState;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberInvitationUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationInfo;
import com.umc.product.member.application.port.in.query.dto.MemberInvitationSearchResult;
import com.umc.product.member.application.port.in.query.dto.SearchMemberInvitationQuery;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadQueryService 멤버/초대 가능")
class CommunityThreadMemberInvitableQueryServiceTest {

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
    SearchMemberInvitationUseCase searchInvitationUseCase;

    CommunityThreadQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadQueryService(
            threadQueryPort,
            getMemberUseCase,
            getChallengerUseCase,
            getGisuUseCase,
            searchInvitationUseCase,
            new CommunityThreadProperties(100)
        );
    }

    @Test
    @DisplayName("Challenger 이력이 없는 멤버를 포함해 배치 조회한 뒤 필터와 안정 정렬 후 페이지한다")
    void listMembers_배치_조회_후_필터_정렬_페이지한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        List<CommunityThreadMemberRow> rows = List.of(
            memberRow(30L, CommunityThreadMemberRole.MEMBER),
            memberRow(10L, CommunityThreadMemberRole.OWNER),
            memberRow(20L, CommunityThreadMemberRole.ADMIN)
        );
        Set<Long> memberIds = Set.of(10L, 20L, 30L);
        given(threadQueryPort.listActiveThreadMembers(1L)).willReturn(rows);
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(Map.of(
            10L, member(10L, "조이"),
            20L, member(20L, "아리"),
            30L, member(30L, "보라")
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            10L, List.of(challenger(10L, ChallengerPart.DESIGN)),
            20L, List.of(challenger(20L, ChallengerPart.DESIGN))
        ));
        given(getGisuUseCase.getByIds(Set.of(20L)))
            .willReturn(List.of(new GisuInfo(20L, 9L, NOW, NOW.plusSeconds(1), true)));

        // when
        ThreadMemberPageInfo result = sut.listMembers(new ListThreadMembersQuery(
            1L, 10L, null, null, ChallengerPart.DESIGN, 9L, 0, 1
        ));

        // then
        assertThat(result.items()).extracting(info -> info.memberId()).containsExactly(10L);
        assertThat(result.nextOffset()).isEqualTo(1);
        assertThat(result.total()).isEqualTo(2L);
        verify(getMemberUseCase).findAllByIds(memberIds);
        verify(getChallengerUseCase).getAllBasicByMemberIds(memberIds);
        verify(getGisuUseCase).getByIds(Set.of(20L));
    }

    @Test
    @DisplayName("지정 멤버 조회는 요청 순서를 보존해 공개 정보를 배치 조립한다")
    void getMembersByIds_요청_순서를_보존하고_배치_조립한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        Set<Long> memberIds = Set.of(10L, 20L, 30L);
        given(threadQueryPort.listActiveThreadMembers(1L)).willReturn(List.of(
            memberRow(10L, CommunityThreadMemberRole.OWNER),
            memberRow(20L, CommunityThreadMemberRole.ADMIN),
            memberRow(30L, CommunityThreadMemberRole.MEMBER)
        ));
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(Map.of(
            10L, member(10L, "조이"),
            20L, member(20L, "아리"),
            30L, member(30L, "보라")
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            10L, List.of(challenger(10L, ChallengerPart.DESIGN)),
            20L, List.of(challenger(20L, ChallengerPart.DESIGN))
        ));
        given(getGisuUseCase.getByIds(Set.of(20L)))
            .willReturn(List.of(new GisuInfo(20L, 9L, NOW, NOW.plusSeconds(1), true)));

        // when
        List<ThreadMemberInfo> result = sut.getMembersByIds(
            new GetThreadMembersByIdsQuery(1L, 10L, List.of(30L, 10L, 20L))
        );

        // then
        assertThat(result).extracting(ThreadMemberInfo::memberId).containsExactly(30L, 10L, 20L);
        assertThat(result).extracting(ThreadMemberInfo::name).containsExactly("보라", "조이", "아리");
        assertThat(result.getFirst().part()).isNull();
        assertThat(result.getFirst().generation()).isNull();
        verify(getMemberUseCase).findAllByIds(memberIds);
        verify(getChallengerUseCase).getAllBasicByMemberIds(memberIds);
        verify(getGisuUseCase).getByIds(Set.of(20L));
    }

    @Test
    @DisplayName("지정 멤버 중 ACTIVE row가 하나라도 없으면 THREAD_MEMBER_NOT_FOUND로 fail closed한다")
    void getMembersByIds_활성_row_누락을_거절한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        given(threadQueryPort.listActiveThreadMembers(1L)).willReturn(List.of(
            memberRow(10L, CommunityThreadMemberRole.OWNER)
        ));

        // when & then
        assertThatThrownBy(() -> sut.getMembersByIds(
            new GetThreadMembersByIdsQuery(1L, 10L, List.of(10L, 20L))
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);
        verifyNoInteractions(getMemberUseCase, getChallengerUseCase, getGisuUseCase);
    }

    @Test
    @DisplayName("Member read model이 누락되면 THREAD_MEMBER_NOT_FOUND로 fail closed한다")
    void getMembersByIds_member_read_model_누락을_거절한다() {
        // given
        Set<Long> memberIds = Set.of(10L, 20L);
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        given(threadQueryPort.listActiveThreadMembers(1L)).willReturn(List.of(
            memberRow(10L, CommunityThreadMemberRole.OWNER),
            memberRow(20L, CommunityThreadMemberRole.MEMBER)
        ));
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(Map.of(10L, member(10L, "조이")));
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            10L, List.of(challenger(10L, ChallengerPart.DESIGN)),
            20L, List.of(challenger(20L, ChallengerPart.DESIGN))
        ));
        given(getGisuUseCase.getByIds(Set.of(20L)))
            .willReturn(List.of(new GisuInfo(20L, 9L, NOW, NOW.plusSeconds(1), true)));

        // when & then
        assertThatThrownBy(() -> sut.getMembersByIds(
            new GetThreadMembersByIdsQuery(1L, 10L, List.of(10L, 20L))
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("지정 멤버 수가 설정된 최대 인원을 넘으면 조회를 거절한다")
    void getMembersByIds_최대_인원을_초과하면_거절한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        List<Long> memberIds = LongStream.rangeClosed(1L, 101L).boxed().toList();

        // when & then
        assertThatThrownBy(() -> sut.getMembersByIds(
            new GetThreadMembersByIdsQuery(1L, 10L, memberIds)
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        verifyNoInteractions(getMemberUseCase, getChallengerUseCase, getGisuUseCase);
    }

    @Test
    @DisplayName("memberIds는 양성 고유 ID의 불변 복사본으로 보존한다")
    void getThreadMembersByIdsQuery_양성_고유_ID를_복사한다() {
        // given
        List<Long> memberIds = new ArrayList<>(List.of(30L, 10L));

        // when
        GetThreadMembersByIdsQuery query = new GetThreadMembersByIdsQuery(1L, 10L, memberIds);
        memberIds.set(0, 99L);

        // then
        assertThat(query.memberIds()).containsExactly(30L, 10L);
        assertThatThrownBy(() -> query.memberIds().add(20L))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new GetThreadMembersByIdsQuery(1L, 10L, List.of(0L)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GetThreadMembersByIdsQuery(1L, 10L, List.of(10L, 10L)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("초대 가능 검색은 OWNER 접근을 확인하고 ACTIVE/KICKED 제외 ID를 페이지 전에 공개 UseCase로 전달한다")
    void searchInvitable_제외_ID를_페이지_전에_위임한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.OWNER)));
        given(threadQueryPort.listInvitationBlockedMemberIds(1L)).willReturn(List.of(10L, 20L));
        given(searchInvitationUseCase.search(org.mockito.ArgumentMatchers.any())).willReturn(
            new MemberInvitationSearchResult(
                List.of(new MemberInvitationInfo(
                    30L, null, "새 멤버", null, null
                )),
                3,
                7L
            )
        );

        // when
        ThreadInvitablePageInfo result = sut.searchInvitable(
            new SearchThreadInvitableQuery(1L, 10L, "  새  ", 2, 1)
        );

        // then
        assertThat(result.items()).extracting(info -> info.memberId()).containsExactly(30L);
        assertThat(result.items().getFirst().challengerId()).isNull();
        assertThat(result.items().getFirst().part()).isNull();
        assertThat(result.items().getFirst().generation()).isNull();
        assertThat(result.nextOffset()).isEqualTo(3);
        assertThat(result.total()).isEqualTo(7L);
        ArgumentCaptor<SearchMemberInvitationQuery> captor =
            ArgumentCaptor.forClass(SearchMemberInvitationQuery.class);
        verify(searchInvitationUseCase).search(captor.capture());
        assertThat(captor.getValue().keyword()).isEqualTo("새");
        assertThat(captor.getValue().excludedMemberIds()).containsExactlyInAnyOrder(10L, 20L);
        assertThat(captor.getValue().offset()).isEqualTo(2);
        assertThat(captor.getValue().limit()).isEqualTo(1);
        verifyNoInteractions(getMemberUseCase, getChallengerUseCase, getGisuUseCase);
    }

    @Test
    @DisplayName("일반 멤버의 초대 가능 검색은 공개 검색 UseCase 호출 전에 거절한다")
    void searchInvitable_일반_멤버를_거절한다() {
        // given
        given(threadQueryPort.findThread(1L, 10L)).willReturn(Optional.of(thread(CommunityThreadMemberRole.MEMBER)));

        // when & then
        assertThatThrownBy(() -> sut.searchInvitable(new SearchThreadInvitableQuery(1L, 10L, null, 0, 20)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(exception -> ((CommunityDomainException) exception).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        verifyNoInteractions(searchInvitationUseCase);
    }

    @Test
    @DisplayName("비멤버 요청자도 삭제되지 않은 스레드의 멤버 목록을 조회할 수 있다")
    void listMembers_비멤버도_허용한다() {
        // given
        given(threadQueryPort.findThread(1L, 99L)).willReturn(Optional.of(nonMemberThread()));
        given(threadQueryPort.listActiveThreadMembers(1L))
            .willReturn(List.of(memberRow(10L, CommunityThreadMemberRole.OWNER)));
        given(getMemberUseCase.findAllByIds(Set.of(10L))).willReturn(Map.of(10L, member(10L, "조이")));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(10L))).willReturn(Map.of());

        // when
        ThreadMemberPageInfo result = sut.listMembers(new ListThreadMembersQuery(
            1L, 99L, null, null, null, null, 0, 20
        ));

        // then
        assertThat(result.items()).extracting(info -> info.memberId()).containsExactly(10L);
        assertThat(result.total()).isEqualTo(1L);
    }

    private CommunityThreadQueryRow nonMemberThread() {
        return new CommunityThreadQueryRow(
            1L, "스레드", null, CommunityThreadCategory.STUDY, "📚",
            3L, 0L, false, false, null, null,
            null, null, null, 10L, NOW, null, NOW, NOW
        );
    }

    private CommunityThreadQueryRow thread(CommunityThreadMemberRole role) {
        return new CommunityThreadQueryRow(
            1L, "스레드", null, CommunityThreadCategory.STUDY, "📚",
            3L, 0L, false, false, role, CommunityThreadMemberState.ACTIVE,
            null, null, null, 10L, NOW, null, NOW, NOW
        );
    }

    private CommunityThreadMemberRow memberRow(Long memberId, CommunityThreadMemberRole role) {
        return new CommunityThreadMemberRow(memberId, role, CommunityThreadMemberState.ACTIVE, NOW);
    }

    private MemberInfo member(Long memberId, String name) {
        return MemberInfo.builder().id(memberId).name(name).build();
    }

    private ChallengerBasicInfo challenger(Long memberId, ChallengerPart part) {
        return new ChallengerBasicInfo(100L + memberId, memberId, 20L, part, List.of(), null);
    }
}
