package com.umc.product.challenger.application.service;

import static com.umc.product.support.fixture.ChallengerUnitFixture.챌린저;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

import com.umc.product.challenger.application.port.in.query.GetChallengerPointUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerPointInfo;
import com.umc.product.challenger.application.port.out.LoadChallengerPort;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.challenger.domain.enums.PointType;
import com.umc.product.challenger.domain.exception.ChallengerDomainException;
import com.umc.product.challenger.domain.exception.ChallengerErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerQueryService 엣지 케이스")
class ChallengerQueryServiceEdgeCaseTest {

    private static final Long CHALLENGER_ID = 10L;
    private static final Long MEMBER_ID = 20L;
    private static final Long GISU_ID = 30L;

    @Mock
    LoadChallengerPort loadChallengerPort;

    @Mock
    GetChallengerPointUseCase getChallengerPointUseCase;

    @InjectMocks
    ChallengerQueryService sut;

    @Nested
    @DisplayName("단건 조회")
    class SingleLookup {

        @Test
        @DisplayName("ID 조회는 상벌점을 합산한 챌린저 정보를 반환한다")
        void id로_상벌점_포함_조회한다() {
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.getById(CHALLENGER_ID)).willReturn(challenger);
            given(getChallengerPointUseCase.getListByChallengerId(CHALLENGER_ID))
                .willReturn(List.of(point(1.5), point(-0.5)));

            ChallengerInfo result = sut.getById(CHALLENGER_ID);

            assertThat(result.totalPoints()).isEqualTo(1.0);
            assertThat(result.challengerPoints()).hasSize(2);
        }

        @Test
        @DisplayName("findById는 존재하면 상벌점 포함 Optional을 반환한다")
        void find_by_id_성공() {
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.findById(CHALLENGER_ID)).willReturn(Optional.of(challenger));
            given(getChallengerPointUseCase.getListByChallengerId(CHALLENGER_ID)).willReturn(List.of());

            assertThat(sut.findById(CHALLENGER_ID)).get().extracting(ChallengerInfo::memberId).isEqualTo(MEMBER_ID);
        }

        @Test
        @DisplayName("findById와 findByIdOrNull은 누락 결과를 각각 empty와 null로 반환한다")
        void 누락_결과를_추가_조회하지_않는다() {
            given(loadChallengerPort.findById(CHALLENGER_ID)).willReturn(Optional.empty());

            assertThat(sut.findById(CHALLENGER_ID)).isEmpty();
            assertThat(sut.findByIdOrNull(CHALLENGER_ID)).isNull();
            then(getChallengerPointUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("findByIdOrNull은 존재하는 챌린저를 상벌점과 함께 반환한다")
        void find_by_id_or_null_성공() {
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.findById(CHALLENGER_ID)).willReturn(Optional.of(challenger));
            given(getChallengerPointUseCase.getListByChallengerId(CHALLENGER_ID)).willReturn(List.of());

            assertThat(sut.findByIdOrNull(CHALLENGER_ID).challengerId()).isEqualTo(CHALLENGER_ID);
        }

        @Test
        @DisplayName("member·gisu 조합이 없으면 CHALLENGER_NOT_FOUND를 던진다")
        void member_gisu_조합_누락을_구분한다() {
            given(loadChallengerPort.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(Optional.empty());

            assertError(
                () -> sut.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID),
                ChallengerErrorCode.CHALLENGER_NOT_FOUND
            );
            assertThat(sut.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).isEmpty();
        }

        @Test
        @DisplayName("활성 챌린저 조회는 비활성 상태를 거부한다")
        void 비활성_챌린저를_거부한다() {
            Challenger graduated = 챌린저(
                CHALLENGER_ID,
                MEMBER_ID,
                GISU_ID,
                ChallengerPart.SPRINGBOOT,
                ChallengerStatus.GRADUATED
            );
            given(loadChallengerPort.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(Optional.of(graduated));

            assertError(
                () -> sut.getActiveByMemberIdAndGisuId(MEMBER_ID, GISU_ID),
                ChallengerErrorCode.CHALLENGER_NOT_ACTIVE
            );
            then(getChallengerPointUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("활성 챌린저 조회는 상벌점 정보를 반환한다")
        void 활성_챌린저를_반환한다() {
            Challenger active = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.findByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
                .willReturn(Optional.of(active));
            given(getChallengerPointUseCase.getListByChallengerId(CHALLENGER_ID)).willReturn(List.of());

            assertThat(sut.getActiveByMemberIdAndGisuId(MEMBER_ID, GISU_ID).challengerStatus())
                .isEqualTo(ChallengerStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("배치 조회")
    class BatchLookup {

        @Test
        @DisplayName("빈 챌린저 목록은 상벌점 배치 조회를 하지 않는다")
        void 빈_목록은_즉시_반환한다() {
            given(loadChallengerPort.getAllByMemberId(MEMBER_ID)).willReturn(List.of());

            assertThat(sut.getAllByMemberId(MEMBER_ID)).isEmpty();
            then(getChallengerPointUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("여러 챌린저의 상벌점을 한 번에 조회하고 누락 ID는 빈 목록으로 처리한다")
        void 상벌점을_배치로_조회한다() {
            Challenger first = 챌린저(1L, MEMBER_ID, GISU_ID);
            Challenger second = 챌린저(2L, MEMBER_ID, GISU_ID + 1);
            given(loadChallengerPort.getAllByMemberId(MEMBER_ID)).willReturn(List.of(first, second));
            given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(1L, 2L)))
                .willReturn(Map.of(1L, List.of(point(2.0))));

            List<ChallengerInfo> result = sut.getAllByMemberId(MEMBER_ID);

            assertThat(result).extracting(ChallengerInfo::totalPoints).containsExactly(2.0, 0.0);
        }

        @Test
        @DisplayName("member ID 집합이 null 또는 비어 있으면 port를 호출하지 않는다")
        void 빈_member_id_집합을_처리한다() {
            assertThat(sut.getAllByMemberIds(null)).isEmpty();
            assertThat(sut.getAllByMemberIds(Set.of())).isEmpty();
            assertThat(sut.getAllBasicByMemberIds(null)).isEmpty();
            assertThat(sut.getAllBasicByMemberIds(Set.of())).isEmpty();
            assertThat(sut.batchGetByMemberIdsAndGisuId(null, GISU_ID)).isEmpty();
            assertThat(sut.listByMemberIdsAndGisuId(Set.of(), GISU_ID)).isEmpty();
            then(loadChallengerPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("port가 빈 목록을 반환하면 member 그룹 Map도 비어 있다")
        void 빈_port_결과를_처리한다() {
            Set<Long> memberIds = Set.of(MEMBER_ID);
            given(loadChallengerPort.listAllByMemberIds(memberIds)).willReturn(List.of());

            assertThat(sut.getAllByMemberIds(memberIds)).isEmpty();
            assertThat(sut.getAllBasicByMemberIds(memberIds)).isEmpty();
        }

        @Test
        @DisplayName("챌린저를 member ID별로 그룹핑하고 기본 정보 조회는 상벌점을 조회하지 않는다")
        void member_id별로_그룹핑한다() {
            Set<Long> memberIds = Set.of(1L, 2L);
            List<Challenger> challengers = List.of(
                챌린저(11L, 1L, GISU_ID),
                챌린저(12L, 1L, GISU_ID + 1),
                챌린저(13L, 2L, GISU_ID)
            );
            given(loadChallengerPort.listAllByMemberIds(memberIds)).willReturn(challengers);
            given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(11L, 12L, 13L)))
                .willReturn(Map.of());

            assertThat(sut.getAllByMemberIds(memberIds)).containsKeys(1L, 2L);
            assertThat(sut.getAllBasicByMemberIds(memberIds).get(1L)).hasSize(2);
        }

        @Test
        @DisplayName("batch get은 member ID를 key로 만들고 상벌점 누락을 빈 목록으로 처리한다")
        void batch_get을_map으로_변환한다() {
            Set<Long> memberIds = Set.of(1L, 2L);
            List<Challenger> challengers = List.of(
                챌린저(11L, 1L, GISU_ID),
                챌린저(12L, 2L, GISU_ID)
            );
            given(loadChallengerPort.batchGetByMemberIdsAndGisuId(memberIds, GISU_ID))
                .willReturn(challengers);
            given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(11L, 12L)))
                .willReturn(Map.of(11L, List.of(point(1.0))));

            Map<Long, ChallengerInfo> result = sut.batchGetByMemberIdsAndGisuId(memberIds, GISU_ID);

            assertThat(result).containsKeys(1L, 2L);
            assertThat(result.get(2L).totalPoints()).isZero();
        }

        @Test
        @DisplayName("graceful list는 결과 없음과 상벌점 누락을 구분해 처리한다")
        void graceful_list를_변환한다() {
            Set<Long> memberIds = Set.of(MEMBER_ID);
            given(loadChallengerPort.listByMemberIdsAndGisuId(memberIds, GISU_ID))
                .willReturn(List.of());
            assertThat(sut.listByMemberIdsAndGisuId(memberIds, GISU_ID)).isEmpty();

            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.listByMemberIdsAndGisuId(memberIds, GISU_ID))
                .willReturn(List.of(challenger));
            given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(CHALLENGER_ID)))
                .willReturn(Map.of());

            assertThat(sut.listByMemberIdsAndGisuId(memberIds, GISU_ID).get(MEMBER_ID).totalPoints()).isZero();
        }
    }

    @Nested
    @DisplayName("상태 및 간소화 조회")
    class StatusAndLightweightLookup {

        @Test
        @DisplayName("최신 챌린저가 탈부 또는 제명이면 작성자로 허용하지 않는다")
        void 탈부와_제명을_거부한다() {
            Challenger withdrawn = 챌린저(
                CHALLENGER_ID,
                MEMBER_ID,
                GISU_ID,
                ChallengerPart.WEB,
                ChallengerStatus.WITHDRAWN
            );
            given(loadChallengerPort.findTopByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(withdrawn);
            assertError(
                () -> sut.getLatestActiveChallengerByMemberId(MEMBER_ID),
                ChallengerErrorCode.NOT_ALLOWED_AUTHOR
            );

            Challenger expelled = 챌린저(
                CHALLENGER_ID,
                MEMBER_ID,
                GISU_ID,
                ChallengerPart.WEB,
                ChallengerStatus.EXPELLED
            );
            given(loadChallengerPort.findTopByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(expelled);
            assertError(
                () -> sut.getLatestActiveChallengerByMemberId(MEMBER_ID),
                ChallengerErrorCode.NOT_ALLOWED_AUTHOR
            );
        }

        @Test
        @DisplayName("활성 최신 챌린저는 상태 정보로 반환한다")
        void 활성_최신_챌린저를_반환한다() {
            given(loadChallengerPort.findTopByMemberIdOrderByCreatedAtDesc(MEMBER_ID))
                .willReturn(챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID));

            assertThat(sut.getLatestActiveChallengerByMemberId(MEMBER_ID).status())
                .isEqualTo(ChallengerStatus.ACTIVE);
        }

        @Test
        @DisplayName("ID 집합은 null·empty를 즉시 반환하고 결과는 map과 list로 변환한다")
        void id_집합을_변환한다() {
            assertThat(sut.getAllByIdsAsMap(null)).isEmpty();
            assertThat(sut.getAllByIds(Set.of())).isEmpty();

            Set<Long> ids = Set.of(CHALLENGER_ID);
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.getAllByIds(ids)).willReturn(List.of(challenger));
            given(getChallengerPointUseCase.getListByChallengerId(CHALLENGER_ID)).willReturn(List.of());

            assertThat(sut.getAllByIdsAsMap(ids)).containsKey(CHALLENGER_ID);
            assertThat(sut.getAllByIds(ids)).extracting(ChallengerInfo::challengerId)
                .containsExactly(CHALLENGER_ID);
        }

        @Test
        @DisplayName("상벌점 제외 조회는 point UseCase를 호출하지 않는다")
        void 간소화_조회는_상벌점을_제외한다() {
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.listByMemberIdsAndGisuId(Set.of(MEMBER_ID), GISU_ID))
                .willReturn(List.of(challenger));
            given(loadChallengerPort.listByChapterId(9L)).willReturn(List.of(challenger));
            given(loadChallengerPort.getAllByGisuId(GISU_ID)).willReturn(List.of(challenger));
            given(loadChallengerPort.findLatestPerMember()).willReturn(List.of(challenger));

            assertThat(sut.listBasicByMemberIdsAndGisuId(Set.of(MEMBER_ID), GISU_ID)).hasSize(1);
            assertThat(sut.listBasicByGisuId(GISU_ID)).hasSize(1);
            assertThat(sut.listByChapterId(9L)).hasSize(1);
            assertThat(sut.getAllByGisuIdWithoutChallengerPoints(GISU_ID)).hasSize(1);
            assertThat(sut.getAllLatestGisuPerMemberWithoutChallengerPoints()).hasSize(1);
            then(getChallengerPointUseCase).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("기수 전체 조회는 상벌점을 배치로 결합한다")
        void 기수_전체를_배치_조회한다() {
            Challenger challenger = 챌린저(CHALLENGER_ID, MEMBER_ID, GISU_ID);
            given(loadChallengerPort.getAllByGisuId(GISU_ID)).willReturn(List.of(challenger));
            given(getChallengerPointUseCase.getMapByChallengerIds(Set.of(CHALLENGER_ID)))
                .willReturn(Map.of(CHALLENGER_ID, List.of(point(3.0))));

            assertThat(sut.getAllByGisuId(GISU_ID).getFirst().totalPoints()).isEqualTo(3.0);
        }
    }

    private static ChallengerPointInfo point(double value) {
        return ChallengerPointInfo.builder()
            .id(1L)
            .challengerId(CHALLENGER_ID)
            .pointType(PointType.CUSTOM)
            .point(value)
            .description("테스트")
            .build();
    }

    private static void assertError(Runnable action, ChallengerErrorCode errorCode) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(ChallengerDomainException.class, exception ->
                assertThat(exception.getBaseCode()).isEqualTo(errorCode)
            );
    }
}
