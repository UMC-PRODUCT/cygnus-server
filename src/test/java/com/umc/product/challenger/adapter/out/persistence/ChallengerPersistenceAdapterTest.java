package com.umc.product.challenger.adapter.out.persistence;

import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.umc.product.challenger.application.port.in.query.dto.SearchChallengerQuery;
import com.umc.product.challenger.application.port.out.dto.ChallengerSearchBundle;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.common.domain.enums.ChallengerPart;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerPersistenceAdapter")
class ChallengerPersistenceAdapterTest {

    @Mock
    ChallengerJpaRepository repository;
    @Mock
    ChallengerQueryRepository queryRepository;

    @InjectMocks
    ChallengerPersistenceAdapter sut;

    @Test
    @DisplayName("ID 조회는 Optional을 그대로 반환한다")
    void ID로_조회한다() {
        Challenger challenger = challenger();
        given(repository.findById(100L)).willReturn(Optional.of(challenger));

        assertThat(sut.findById(100L)).containsSame(challenger);
        assertThat(sut.getById(100L)).isSameAs(challenger);
    }

    @Test
    @DisplayName("필수 ID 조회 결과가 없으면 챌린저 없음 예외를 던진다")
    void 필수_ID_조회_누락을_거부한다() {
        given(repository.findById(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getById(100L))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("회원·기수 조회와 회원/기수 목록 조회를 repository에 위임한다")
    void 기본_목록_조회를_위임한다() {
        Challenger challenger = challenger();
        given(repository.findByMemberIdAndGisuId(1L, 20L)).willReturn(Optional.of(challenger));
        given(repository.findByMemberId(1L)).willReturn(List.of(challenger));
        given(repository.findByGisuId(20L)).willReturn(List.of(challenger));
        given(repository.findByGisuIdIn(List.of(20L))).willReturn(List.of(challenger));
        given(repository.findByIdIn(Set.of(100L))).willReturn(List.of(challenger));

        assertThat(sut.findByMemberIdAndGisuId(1L, 20L)).containsSame(challenger);
        assertThat(sut.getAllByMemberId(1L)).containsExactly(challenger);
        assertThat(sut.getAllByGisuId(20L)).containsExactly(challenger);
        assertThat(sut.getAllByGisuIds(List.of(20L))).containsExactly(challenger);
        assertThat(sut.getAllByIds(Set.of(100L))).containsExactly(challenger);
    }

    @Test
    @DisplayName("회원 ID가 null이면 존재 조회를 하지 않고 false를 반환한다")
    void null_회원은_존재하지_않는다() {
        assertThat(sut.existsByMemberId(null)).isFalse();
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원 ID가 있으면 repository의 존재 결과를 반환한다")
    void 회원_존재를_조회한다() {
        given(repository.existsByMemberId(1L)).willReturn(true);

        assertThat(sut.existsByMemberId(1L)).isTrue();
    }

    @Test
    @DisplayName("회원 ID 집합이 null 또는 비어 있으면 목록 조회를 생략한다")
    void 빈_회원_ID_목록을_처리한다() {
        assertThat(sut.listAllByMemberIds(null)).isEmpty();
        assertThat(sut.listAllByMemberIds(Set.of())).isEmpty();
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("회원 ID 집합으로 모든 챌린저를 조회한다")
    void 회원_ID_목록으로_조회한다() {
        given(repository.findByMemberIdIn(Set.of(1L))).willReturn(List.of(challenger()));

        assertThat(sut.listAllByMemberIds(Set.of(1L))).hasSize(1);
    }

    @Test
    @DisplayName("지부 목록 조회와 집합 크기 계산을 수행한다")
    void 지부_조회와_ID_개수를_계산한다() {
        given(queryRepository.listByChapterId(30L)).willReturn(List.of(challenger()));

        assertThat(sut.listByChapterId(30L)).hasSize(1);
        assertThat(sut.countByIdIn(Set.of(100L, 101L))).isEqualTo(2L);
    }

    @Test
    @DisplayName("회원의 최신 챌린저가 없으면 예외를 던진다")
    void 최신_챌린저_누락을_거부한다() {
        given(repository.findTopByMemberIdOrderByCreatedAtDesc(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.findTopByMemberIdOrderByCreatedAtDesc(1L))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("회원의 최신 챌린저를 반환한다")
    void 최신_챌린저를_조회한다() {
        Challenger challenger = challenger();
        given(repository.findTopByMemberIdOrderByCreatedAtDesc(1L)).willReturn(Optional.of(challenger));

        assertThat(sut.findTopByMemberIdOrderByCreatedAtDesc(1L)).isSameAs(challenger);
    }

    @Test
    @DisplayName("batch 필수 조회는 null·빈 회원 집합이면 repository를 호출하지 않는다")
    void 빈_batch_필수_조회를_처리한다() {
        assertThat(sut.batchGetByMemberIdsAndGisuId(null, 20L)).isEmpty();
        assertThat(sut.batchGetByMemberIdsAndGisuId(Set.of(), 20L)).isEmpty();
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("batch 필수 조회에서 일부 회원이 누락되면 예외를 던진다")
    void batch_필수_조회_누락을_거부한다() {
        Set<Long> memberIds = Set.of(1L, 2L);
        given(repository.findByMemberIdInAndGisuId(memberIds, 20L))
            .willReturn(List.of(challenger()));

        assertThatThrownBy(() -> sut.batchGetByMemberIdsAndGisuId(memberIds, 20L))
            .isInstanceOf(ChallengerDomainException.class);
    }

    @Test
    @DisplayName("batch 필수 조회에서 모든 회원이 존재하면 목록을 반환한다")
    void batch_필수_조회를_수행한다() {
        Set<Long> memberIds = Set.of(1L, 2L);
        List<Challenger> challengers = List.of(
            챌린저(100L, 1L, 20L),
            챌린저(101L, 2L, 20L)
        );
        given(repository.findByMemberIdInAndGisuId(memberIds, 20L)).willReturn(challengers);

        assertThat(sut.batchGetByMemberIdsAndGisuId(memberIds, 20L)).isSameAs(challengers);
    }

    @Test
    @DisplayName("선택 조회는 회원 집합 또는 기수가 없으면 빈 목록을 반환한다")
    void 선택_조회_입력_누락을_처리한다() {
        assertThat(sut.listByMemberIdsAndGisuId(null, 20L)).isEmpty();
        assertThat(sut.listByMemberIdsAndGisuId(Set.of(), 20L)).isEmpty();
        assertThat(sut.listByMemberIdsAndGisuId(Set.of(1L), null)).isEmpty();
        then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("선택 조회는 회원 집합과 기수로 목록을 조회한다")
    void 선택_조회를_수행한다() {
        given(repository.findByMemberIdInAndGisuId(Set.of(1L), 20L))
            .willReturn(List.of(challenger()));

        assertThat(sut.listByMemberIdsAndGisuId(Set.of(1L), 20L)).hasSize(1);
    }

    @Test
    @DisplayName("최신 목록·검색·집계를 query repository에 위임한다")
    @SuppressWarnings("unchecked")
    void 검색과_집계를_위임한다() {
        SearchChallengerQuery query = mock(SearchChallengerQuery.class);
        Pageable pageable = mock(Pageable.class);
        Page<Challenger> page = mock(Page.class);
        ChallengerSearchBundle bundle = mock(ChallengerSearchBundle.class);
        Map<ChallengerPart, Long> counts = Map.of(ChallengerPart.SPRINGBOOT, 1L);
        Map<Long, Double> points = Map.of(100L, 3.0);
        given(queryRepository.getAllLatestGisuPerMember()).willReturn(List.of(challenger()));
        given(queryRepository.pagingSearch(query, pageable)).willReturn(page);
        given(queryRepository.cursorSearch(query, 100L, 20)).willReturn(List.of(challenger()));
        given(queryRepository.countByPart(query)).willReturn(counts);
        given(queryRepository.sumPointsByChallengerIds(Set.of(100L))).willReturn(points);
        given(queryRepository.cursorSearchWithCounts(query, 100L, 20)).willReturn(bundle);
        given(queryRepository.pagingSearchWithCounts(query, pageable)).willReturn(bundle);

        assertThat(sut.findLatestPerMember()).hasSize(1);
        assertThat(sut.search(query, pageable)).isSameAs(page);
        assertThat(sut.cursorSearch(query, 100L, 20)).hasSize(1);
        assertThat(sut.countByPart(query)).isSameAs(counts);
        assertThat(sut.sumPointsByChallengerIds(Set.of(100L))).isSameAs(points);
        assertThat(sut.cursorSearchWithCounts(query, 100L, 20)).isSameAs(bundle);
        assertThat(sut.pagingSearchWithCounts(query, pageable)).isSameAs(bundle);
    }

    @Test
    @DisplayName("저장·일괄 저장·삭제를 repository에 위임한다")
    void 변경을_위임한다() {
        Challenger challenger = challenger();
        List<Challenger> challengers = List.of(challenger);
        given(repository.save(challenger)).willReturn(challenger);
        given(repository.saveAll(challengers)).willReturn(challengers);

        assertThat(sut.save(challenger)).isSameAs(challenger);
        assertThat(sut.saveAll(challengers)).isSameAs(challengers);
        sut.delete(challenger);

        then(repository).should().delete(challenger);
    }

    private Challenger challenger() {
        return 챌린저(100L, 1L, 20L);
    }
}
