package com.umc.product.challenger.application.service;

import static com.umc.product.support.fixture.ChallengerUnitFixture.검색_행;
import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
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

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.GlobalSearchChallengerCursorResult;
import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerCursorResult;
import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerResult;
import com.umc.product.challenger.application.port.out.SearchChallengerPort;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchBundle;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchRow;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerSearchService")
class ChallengerSearchServiceTest {

    private static final SearchChallengerQuery QUERY =
        new SearchChallengerQuery(null, null, null, "길동", null, null, null, null, List.of());

    @Mock
    SearchChallengerPort searchChallengerPort;

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetChallengerRoleUseCase getChallengerRoleUseCase;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    GetChallengerPointUseCase getChallengerPointUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @InjectMocks
    ChallengerSearchService sut;

    @Nested
    @DisplayName("오프셋 검색")
    class OffsetSearch {

        @Test
        @DisplayName("빈 검색 결과는 부가 정보를 조회하지 않고 파트 집계만 반환한다")
        void 빈_검색_결과를_처리한다() {
            Pageable pageable = PageRequest.of(0, 2);
            given(searchChallengerPort.pagingSearchWithCounts(QUERY, pageable))
                .willReturn(new ChallengerSearchBundle(List.of(), Map.of(ChallengerPart.WEB, 0L)));

            SearchChallengerResult result = sut.offsetSearch(QUERY, pageable);

            assertThat(result.page()).isEmpty();
            assertThat(result.partCounts()).containsEntry(ChallengerPart.WEB, 0L);
            then(getChallengerRoleUseCase).shouldHaveNoInteractions();
            then(getGisuUseCase).shouldHaveNoInteractions();
            then(getFileUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("검색 행을 상벌점·역할·기수·프로필 링크와 배치로 결합한다")
        void 부가_정보를_배치로_결합한다() {
            Pageable pageable = PageRequest.of(0, 2);
            ChallengerSearchRow first = 검색_행(1L, 11L, 21L, "image-1");
            ChallengerSearchRow second = 검색_행(2L, 12L, 21L, null);
            given(searchChallengerPort.pagingSearchWithCounts(QUERY, pageable))
                .willReturn(new ChallengerSearchBundle(
                    List.of(first, second),
                    Map.of(ChallengerPart.SPRINGBOOT, 2L, ChallengerPart.WEB, 3L)
                ));
            given(searchChallengerPort.sumPointsByChallengerIds(Set.of(1L, 2L)))
                .willReturn(Map.of(1L, 2.5));
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(Set.of(1L, 2L)))
                .willReturn(Map.of(1L, List.of(ChallengerRoleType.SCHOOL_PRESIDENT)));
            given(getGisuUseCase.getByIds(Set.of(21L)))
                .willReturn(List.of(new GisuInfo(21L, 12L, null, null, false)));
            given(getFileUseCase.getFileLinks(List.of("image-1")))
                .willReturn(Map.of("image-1", "https://cdn.example/profile.png"));

            SearchChallengerResult result = sut.offsetSearch(QUERY, pageable);

            assertThat(result.page().getTotalElements()).isEqualTo(5L);
            assertThat(result.page().getContent().getFirst().pointSum()).isEqualTo(2.5);
            assertThat(result.page().getContent().getFirst().generation()).isEqualTo(12L);
            assertThat(result.page().getContent().getFirst().profileImageLink())
                .isEqualTo("https://cdn.example/profile.png");
            assertThat(result.page().getContent().getLast().pointSum()).isZero();
            assertThat(result.page().getContent().getLast().roleTypes()).isEmpty();
            assertThat(result.page().getContent().getLast().profileImageLink()).isNull();
        }
    }

    @Nested
    @DisplayName("커서 검색")
    class CursorSearch {

        @Test
        @DisplayName("size보다 한 건 많이 조회해 다음 페이지와 next cursor를 판별한다")
        void 다음_페이지가_있는_경우() {
            List<ChallengerSearchRow> rows = List.of(
                검색_행(1L, 11L, 21L, "same-image"),
                검색_행(2L, 12L, 22L, "same-image"),
                검색_행(3L, 13L, 23L, "excluded-image")
            );
            given(searchChallengerPort.cursorSearchWithCounts(QUERY, 0L, 2))
                .willReturn(new ChallengerSearchBundle(rows, Map.of()));
            given(searchChallengerPort.sumPointsByChallengerIds(Set.of(1L, 2L))).willReturn(Map.of());
            given(getChallengerRoleUseCase.getAllRoleTypesByChallengerIds(Set.of(1L, 2L))).willReturn(Map.of());
            given(getGisuUseCase.getByIds(Set.of(21L, 22L))).willReturn(List.of(
                new GisuInfo(21L, 11L, null, null, false),
                new GisuInfo(22L, 12L, null, null, false)
            ));
            given(getFileUseCase.getFileLinks(List.of("same-image"))).willReturn(Map.of());

            SearchChallengerCursorResult result = sut.cursorSearch(QUERY, 0L, 2);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(2L);
            assertThat(result.content()).hasSize(2);
            then(getFileUseCase).should().getFileLinks(List.of("same-image"));
        }

        @Test
        @DisplayName("결과가 size 이하이면 next cursor를 반환하지 않는다")
        void 다음_페이지가_없는_경우() {
            given(searchChallengerPort.cursorSearchWithCounts(QUERY, null, 2))
                .willReturn(new ChallengerSearchBundle(List.of(), Map.of()));

            SearchChallengerCursorResult result = sut.cursorSearch(QUERY, null, 2);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.content()).isEmpty();
        }

        @Test
        @DisplayName("글로벌 커서 검색도 size+1 규칙과 프로필 링크 배치를 동일하게 적용한다")
        @SuppressWarnings("removal")
        void 글로벌_커서_검색을_변환한다() {
            List<ChallengerSearchRow> rows = List.of(
                검색_행(1L, 11L, 21L, "image-1"),
                검색_행(2L, 12L, 22L, null)
            );
            given(searchChallengerPort.cursorSearchWithCounts(QUERY, 0L, 1))
                .willReturn(new ChallengerSearchBundle(rows, Map.of()));
            given(getGisuUseCase.getByIds(Set.of(21L)))
                .willReturn(List.of(new GisuInfo(21L, 11L, null, null, false)));
            given(getFileUseCase.getFileLinks(List.of("image-1")))
                .willReturn(Map.of("image-1", "https://cdn.example/image"));

            GlobalSearchChallengerCursorResult result = sut.globalCursorSearch(QUERY, 0L, 1);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo(1L);
            assertThat(result.content().getFirst().gisu()).isEqualTo(11L);
            assertThat(result.content().getFirst().profileImageLink()).isEqualTo("https://cdn.example/image");
        }
    }

    @Nested
    @DisplayName("V2 호환 검색")
    class V2Search {

        @Test
        @DisplayName("오프셋 V2는 각 챌린저의 상벌점을 결합해 Page를 변환한다")
        void offset_v2를_변환한다() {
            Pageable pageable = PageRequest.of(0, 10);
            Challenger challenger = 챌린저(1L, 11L, 21L);
            given(searchChallengerPort.search(QUERY, pageable))
                .willReturn(new PageImpl<>(List.of(challenger), pageable, 1));
            given(getChallengerPointUseCase.getListByChallengerId(1L)).willReturn(List.of());

            assertThat(sut.searchV2(QUERY, pageable).getContent())
                .extracting(ChallengerInfo::challengerId)
                .containsExactly(1L);
        }

        @Test
        @DisplayName("커서 V2는 port 순서를 유지하며 상벌점을 결합한다")
        void cursor_v2를_변환한다() {
            Challenger first = 챌린저(1L, 11L, 21L);
            Challenger second = 챌린저(2L, 12L, 21L);
            given(searchChallengerPort.cursorSearch(QUERY, 5L, 2)).willReturn(List.of(first, second));
            given(getChallengerPointUseCase.getListByChallengerId(1L)).willReturn(List.of());
            given(getChallengerPointUseCase.getListByChallengerId(2L)).willReturn(List.of());

            assertThat(sut.searchV2(QUERY, 5L, 2))
                .extracting(ChallengerInfo::challengerId)
                .containsExactly(1L, 2L);
        }
    }
}
