package com.umc.product.organization.application.service;

import static com.umc.product.support.fixture.OrganizationUnitFixture.리더십;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드;
import static com.umc.product.support.fixture.OrganizationUnitFixture.스쿼드_참여;
import static com.umc.product.support.fixture.OrganizationUnitFixture.챕터_소속;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_멤버;
import static com.umc.product.support.fixture.OrganizationUnitFixture.프로덕트_챕터;
import static com.umc.product.support.fixture.OrganizationUnitFixture.활동_기간;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberInfo;
import com.umc.product.organization.application.port.in.query.dto.umcproduct.UmcProductMemberSearchCondition;
import com.umc.product.organization.application.port.out.query.LoadUmcProductChapterMembershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductLeadershipPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberActivityPeriodPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductMemberPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadParticipantPort;
import com.umc.product.organization.application.port.out.query.LoadUmcProductSquadPort;
import com.umc.product.organization.domain.UmcProductChapter;
import com.umc.product.organization.domain.UmcProductChapterMembership;
import com.umc.product.organization.domain.UmcProductLeadership;
import com.umc.product.organization.domain.UmcProductMember;
import com.umc.product.organization.domain.UmcProductMemberActivityPeriod;
import com.umc.product.organization.domain.UmcProductSquad;
import com.umc.product.organization.domain.UmcProductSquadParticipant;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("UMC PRODUCT 멤버 조회 서비스")
class UmcProductMemberQueryServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END = LocalDate.of(2026, 12, 31);

    @Mock
    LoadUmcProductMemberPort loadUmcProductMemberPort;

    @Mock
    LoadUmcProductMemberActivityPeriodPort loadUmcProductMemberActivityPeriodPort;

    @Mock
    LoadUmcProductChapterMembershipPort loadUmcProductChapterMembershipPort;

    @Mock
    LoadUmcProductLeadershipPort loadUmcProductLeadershipPort;

    @Mock
    LoadUmcProductSquadParticipantPort loadUmcProductSquadParticipantPort;

    @Mock
    LoadUmcProductSquadPort loadUmcProductSquadPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @InjectMocks
    UmcProductMemberQueryService sut;

    @Nested
    @DisplayName("단건 조회")
    class SingleLookup {

        @Test
        @DisplayName("멤버 정보와 모든 활동 이력·스쿼드·프로필 링크를 결합한다")
        void 전체_정보를_결합한다() {
            Graph graph = graph("product-image");
            given(loadUmcProductMemberPort.getById(1L)).willReturn(graph.member);
            given(getMemberUseCase.findById(101L)).willReturn(Optional.of(memberInfo(101L)));
            given(loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberId(1L))
                .willReturn(List.of(graph.period));
            given(loadUmcProductChapterMembershipPort.listByUmcProductMemberId(1L))
                .willReturn(List.of(graph.membership));
            given(loadUmcProductLeadershipPort.listByUmcProductMemberId(1L))
                .willReturn(List.of(graph.leadership));
            given(loadUmcProductSquadParticipantPort.listByUmcProductMemberId(1L))
                .willReturn(List.of(graph.participant));
            given(loadUmcProductSquadPort.listByIds(Set.of(51L))).willReturn(List.of(graph.squad));
            given(getFileUseCase.getFileLinks(List.of("product-image")))
                .willReturn(Map.of("product-image", "https://cdn.example/product.png"));

            UmcProductMemberInfo result = sut.getById(1L);

            assertThat(result.memberName()).isEqualTo("홍길동");
            assertThat(result.umcProductProfileImageUrl()).isEqualTo("https://cdn.example/product.png");
            assertThat(result.activityPeriods()).hasSize(1);
            assertThat(result.chapterMemberships()).hasSize(1);
            assertThat(result.productLeaderships()).hasSize(1);
            assertThat(result.squadParticipations()).hasSize(1);
            assertThat(result.squadParticipations().getFirst().squad().squadId()).isEqualTo(51L);
        }

        @Test
        @DisplayName("원본 회원과 PRODUCT 프로필이 누락되어도 null 필드로 graceful하게 조회한다")
        void 누락_연결_정보를_null로_처리한다() {
            Graph graph = graph(null);
            given(loadUmcProductMemberPort.getById(1L)).willReturn(graph.member);
            given(getMemberUseCase.findById(101L)).willReturn(Optional.empty());
            given(loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberId(1L)).willReturn(List.of());
            given(loadUmcProductChapterMembershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
            given(loadUmcProductLeadershipPort.listByUmcProductMemberId(1L)).willReturn(List.of());
            given(loadUmcProductSquadParticipantPort.listByUmcProductMemberId(1L)).willReturn(List.of());

            UmcProductMemberInfo result = sut.getById(1L);

            assertThat(result.memberName()).isNull();
            assertThat(result.memberNickname()).isNull();
            assertThat(result.memberSchoolName()).isNull();
            assertThat(result.umcProductProfileImageUrl()).isNull();
            then(loadUmcProductSquadPort).shouldHaveNoInteractions();
            then(getFileUseCase).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("검색")
    class Search {

        @Test
        @DisplayName("빈 ID page는 후속 배치 조회 없이 빈 page를 반환한다")
        void 빈_id_page를_즉시_반환한다() {
            Pageable pageable = PageRequest.of(0, 20);
            UmcProductMemberSearchCondition condition = condition(null);
            given(loadUmcProductMemberPort.searchIds(condition, pageable))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

            assertThat(sut.search(condition, pageable)).isEmpty();
            then(loadUmcProductMemberActivityPeriodPort).shouldHaveNoInteractions();
            then(getMemberUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ID page 순서를 유지하고 없어진 entity ID는 건너뛴다")
        void id_page_순서와_누락_entity를_처리한다() {
            Pageable pageable = PageRequest.of(0, 2);
            UmcProductMemberSearchCondition condition = condition(null);
            Graph graph = graph("same-image");
            given(loadUmcProductMemberPort.searchIds(condition, pageable))
                .willReturn(new PageImpl<>(List.of(999L, 1L), pageable, 2));
            given(loadUmcProductMemberPort.listByIds(List.of(999L, 1L))).willReturn(List.of(graph.member));
            given(loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberIds(List.of(999L, 1L)))
                .willReturn(List.of());
            given(loadUmcProductChapterMembershipPort.listByUmcProductMemberIds(List.of(999L, 1L)))
                .willReturn(List.of());
            given(loadUmcProductLeadershipPort.listByUmcProductMemberIds(List.of(999L, 1L)))
                .willReturn(List.of());
            given(loadUmcProductSquadParticipantPort.listByUmcProductMemberIds(List.of(999L, 1L)))
                .willReturn(List.of());
            given(getMemberUseCase.findAllByIds(Set.of(101L))).willReturn(Map.of(101L, memberInfo(101L)));
            given(getFileUseCase.getFileLinks(List.of("same-image"))).willReturn(Map.of());

            assertThat(sut.search(condition, pageable).getContent())
                .extracting(UmcProductMemberInfo::umcProductMemberId)
                .containsExactly(1L);
        }

        @Test
        @DisplayName("activeOn으로 이력을 필터링하고 최근 시작일·ID 역순으로 정렬한다")
        void active_on으로_필터링하고_정렬한다() {
            LocalDate activeOn = LocalDate.of(2026, 8, 1);
            Pageable pageable = PageRequest.of(0, 10);
            UmcProductMemberSearchCondition condition = condition(activeOn);
            UmcProductMember member = 프로덕트_멤버(1L, 101L, "image");
            UmcProductMemberActivityPeriod oldPeriod = 활동_기간(
                10L,
                member,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31)
            );
            UmcProductMemberActivityPeriod currentPeriod = 활동_기간(20L, member, START, END);
            UmcProductChapter chapter = 프로덕트_챕터(31L);
            UmcProductChapterMembership oldMembership = 챕터_소속(
                40L,
                oldPeriod,
                chapter,
                oldPeriod.getStartDate(),
                oldPeriod.getEndDate()
            );
            UmcProductChapterMembership currentMembership = 챕터_소속(
                41L,
                currentPeriod,
                chapter,
                LocalDate.of(2026, 2, 1),
                END
            );
            UmcProductLeadership oldLeadership = 리더십(
                42L,
                oldPeriod,
                oldPeriod.getStartDate(),
                oldPeriod.getEndDate()
            );
            UmcProductLeadership currentLeadership = 리더십(
                43L,
                currentPeriod,
                LocalDate.of(2026, 3, 1),
                END
            );
            UmcProductSquad squad = 스쿼드(51L, START, END);
            UmcProductSquadParticipant currentParticipant = 스쿼드_참여(
                60L,
                squad,
                currentPeriod,
                LocalDate.of(2026, 4, 1),
                END
            );
            given(loadUmcProductMemberPort.searchIds(condition, pageable))
                .willReturn(new PageImpl<>(List.of(1L), pageable, 1));
            given(loadUmcProductMemberPort.listByIds(List.of(1L))).willReturn(List.of(member));
            given(loadUmcProductMemberActivityPeriodPort.listByUmcProductMemberIds(List.of(1L)))
                .willReturn(List.of(oldPeriod, currentPeriod));
            given(loadUmcProductChapterMembershipPort.listByUmcProductMemberIds(List.of(1L)))
                .willReturn(List.of(oldMembership, currentMembership));
            given(loadUmcProductLeadershipPort.listByUmcProductMemberIds(List.of(1L)))
                .willReturn(List.of(oldLeadership, currentLeadership));
            given(loadUmcProductSquadParticipantPort.listByUmcProductMemberIds(List.of(1L)))
                .willReturn(List.of(currentParticipant));
            given(loadUmcProductSquadPort.listByIds(Set.of(51L))).willReturn(List.of(squad));
            given(getMemberUseCase.findAllByIds(Set.of(101L))).willReturn(Map.of());
            given(getFileUseCase.getFileLinks(List.of("image"))).willReturn(Map.of());

            UmcProductMemberInfo result = sut.search(condition, pageable).getContent().getFirst();

            assertThat(result.activityPeriods()).extracting(item -> item.activityPeriodId())
                .containsExactly(20L);
            assertThat(result.chapterMemberships()).extracting(item -> item.chapterMembershipId())
                .containsExactly(41L);
            assertThat(result.productLeaderships()).extracting(item -> item.leadershipId())
                .containsExactly(43L);
            assertThat(result.squadParticipations()).extracting(item -> item.squadParticipantId())
                .containsExactly(60L);
            assertThat(result.memberName()).isNull();
        }
    }

    private Graph graph(String profileImageId) {
        UmcProductMember member = 프로덕트_멤버(1L, 101L, profileImageId);
        UmcProductMemberActivityPeriod period = 활동_기간(11L, member, START, END);
        UmcProductChapter chapter = 프로덕트_챕터(31L);
        UmcProductChapterMembership membership = 챕터_소속(41L, period, chapter, START, END);
        UmcProductLeadership leadership = 리더십(42L, period, START, END);
        UmcProductSquad squad = 스쿼드(51L, START, END);
        UmcProductSquadParticipant participant = 스쿼드_참여(61L, squad, period, START, END);
        return new Graph(member, period, membership, leadership, squad, participant);
    }

    private MemberInfo memberInfo(Long memberId) {
        return MemberInfo.builder()
            .id(memberId)
            .name("홍길동")
            .nickname("길동")
            .schoolName("테스트대학교")
            .profileImageId("member-image")
            .profileImageLink("https://cdn.example/member.png")
            .build();
    }

    private UmcProductMemberSearchCondition condition(LocalDate activeOn) {
        return UmcProductMemberSearchCondition.of(null, null, null, null, activeOn);
    }

    private record Graph(
        UmcProductMember member,
        UmcProductMemberActivityPeriod period,
        UmcProductChapterMembership membership,
        UmcProductLeadership leadership,
        UmcProductSquad squad,
        UmcProductSquadParticipant participant
    ) {
    }
}
