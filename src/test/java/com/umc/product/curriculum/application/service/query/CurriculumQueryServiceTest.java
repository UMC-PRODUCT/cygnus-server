package com.umc.product.curriculum.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.GisuLearningType;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumOverviewInfo;
import com.umc.product.curriculum.application.port.in.query.dto.CurriculumProjection;
import com.umc.product.curriculum.application.port.in.query.dto.MyCurriculumInfo;
import com.umc.product.curriculum.application.port.out.LoadChallengerWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadCurriculumPort;
import com.umc.product.curriculum.application.port.out.LoadMissionFeedbackPort;
import com.umc.product.curriculum.application.port.out.LoadMissionSubmissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookMissionPort;
import com.umc.product.curriculum.application.port.out.LoadOriginalWorkbookPort;
import com.umc.product.curriculum.application.port.out.LoadWeeklyCurriculumPort;
import com.umc.product.curriculum.domain.ChallengerWorkbook;
import com.umc.product.curriculum.domain.Curriculum;
import com.umc.product.curriculum.domain.OriginalWorkbook;
import com.umc.product.curriculum.domain.OriginalWorkbookMission;
import com.umc.product.curriculum.domain.WeeklyCurriculum;
import com.umc.product.curriculum.domain.enums.MissionType;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
class CurriculumQueryServiceTest {

    private static final Long GISU_ID = 9L;
    private static final Long MEMBER_ID = 1L;
    private static final ChallengerPart PART = ChallengerPart.SPRINGBOOT;
    private static final Instant WEEK1_START = Instant.parse("2024-03-01T00:00:00Z");
    private static final Instant WEEK1_END = Instant.parse("2024-03-07T23:59:59Z");
    private static final Instant WEEK2_START = Instant.parse("2024-03-08T00:00:00Z");
    private static final Instant WEEK2_END = Instant.parse("2024-03-14T23:59:59Z");
    @Mock
    GetChallengerUseCase getChallengerUseCase;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    LoadCurriculumPort loadCurriculumPort;
    @Mock
    LoadWeeklyCurriculumPort loadWeeklyCurriculumPort;
    @Mock
    LoadOriginalWorkbookPort loadOriginalWorkbookPort;
    @Mock
    LoadOriginalWorkbookMissionPort loadOriginalWorkbookMissionPort;
    @Mock
    LoadChallengerWorkbookPort loadChallengerWorkbookPort;
    @Mock
    LoadMissionSubmissionPort loadMissionSubmissionPort;
    @Mock
    LoadMissionFeedbackPort loadMissionFeedbackPort;
    @InjectMocks
    CurriculumQueryService sut;
    private CurriculumProjection projection;
    private Curriculum curriculum;

    @BeforeEach
    void setUp() {
        given(getGisuUseCase.getById(GISU_ID)).willReturn(
            new GisuInfo(GISU_ID, GISU_ID, Instant.EPOCH, Instant.MAX, true));
        curriculum = Curriculum.create(GISU_ID, PART, "9기 스프링부트");
        ReflectionTestUtils.setField(curriculum, "id", 100L);
        projection = new CurriculumProjection(100L, PART, "9기 스프링부트");
    }

    // ===== getCurriculumOverview =====

    @Test
    void 복수_기본_트랙_수강자는_조회할_트랙을_명시해야_한다() {
        // given
        트랙_수강을_준비한다(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER));

        // when / then
        assertThatThrownBy(() -> sut.getMyProgress(MEMBER_ID, GISU_ID))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.CURRICULUM_TRACK_REQUIRED);
    }

    @Test
    void 기본_트랙_하나와_플러스를_수강하면_기본_트랙을_자동_선택한다() {
        // given
        트랙_수강을_준비한다(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.INFRA_PLUS));
        given(loadCurriculumPort.getByGisuIdAndTrack(GISU_ID, ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .willReturn(new CurriculumProjection(300L, null, ChallengerTrack.WEB_PRODUCT_ENGINEER, "웹"));
        given(loadWeeklyCurriculumPort.findByCurriculumId(300L, null)).willReturn(List.of());

        // when
        var result = sut.getMyProgress(MEMBER_ID, GISU_ID);

        // then
        assertThat(result.curriculumId()).isEqualTo(300L);
        assertThat(result.track()).isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
        assertThat(result.part()).isNull();
    }

    @Test
    void 복수_트랙_중_선택한_트랙의_진행현황만_반환한다() {
        // given
        트랙_수강을_준비한다(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER));
        given(loadCurriculumPort.getByGisuIdAndTrack(GISU_ID, ChallengerTrack.MOBILE_PRODUCT_ENGINEER))
            .willReturn(new CurriculumProjection(400L, null, ChallengerTrack.MOBILE_PRODUCT_ENGINEER, "모바일"));
        given(loadWeeklyCurriculumPort.findByCurriculumId(400L, null)).willReturn(List.of());

        // when
        var result = sut.getMyProgress(MEMBER_ID, GISU_ID, ChallengerTrack.MOBILE_PRODUCT_ENGINEER);

        // then
        assertThat(result.curriculumId()).isEqualTo(400L);
        assertThat(result.track()).isEqualTo(ChallengerTrack.MOBILE_PRODUCT_ENGINEER);
    }

    @Test
    void 미수강_트랙의_진행현황은_조회하지_못한다() {
        // given
        트랙_수강을_준비한다(List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER));

        // when / then
        assertThatThrownBy(() -> sut.getMyProgress(MEMBER_ID, GISU_ID, ChallengerTrack.DESIGN))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
    }

    @Test
    void 파트_기수에_트랙_선택자를_보내면_거부한다() {
        // given
        given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
            .willReturn(ChallengerInfo.builder().part(PART).build());

        // when / then
        assertThatThrownBy(() -> sut.getMyProgress(MEMBER_ID, GISU_ID, ChallengerTrack.WEB_PRODUCT_ENGINEER))
            .isInstanceOf(CurriculumDomainException.class)
            .extracting("baseCode").isEqualTo(CurriculumErrorCode.INVALID_CURRICULUM_LEARNING_TYPE);
    }

    private void 트랙_수강을_준비한다(List<ChallengerTrack> tracks) {
        given(getGisuUseCase.getById(GISU_ID)).willReturn(
            new GisuInfo(GISU_ID, GISU_ID, Instant.EPOCH, Instant.MAX, true, GisuLearningType.TRACK));
        given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID))
            .willReturn(ChallengerInfo.builder().memberId(MEMBER_ID).gisuId(GISU_ID).tracks(tracks)
                .challengerStatus(ChallengerStatus.ACTIVE).build());
    }

    @Nested
    @DisplayName("커리큘럼 개요 조회")
    class GetCurriculumOverview {

        @Test
        void 전체_주차_조회에_성공한다() {
            // given
            WeeklyCurriculum wc1 = WeeklyCurriculum.create(curriculum, 1L, false, "1주차", WEEK1_START, WEEK1_END);
            WeeklyCurriculum wc2 = WeeklyCurriculum.create(curriculum, 2L, false, "2주차", WEEK2_START, WEEK2_END);
            ReflectionTestUtils.setField(wc1, "id", 10L);
            ReflectionTestUtils.setField(wc2, "id", 20L);

            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1, wc2));

            // when
            CurriculumOverviewInfo result = sut.getCurriculumOverview(GISU_ID, PART, null);

            // then
            assertThat(result.curriculumId()).isEqualTo(100L);
            assertThat(result.title()).isEqualTo("9기 스프링부트");
            assertThat(result.weeks()).hasSize(2);

            CurriculumOverviewInfo.WeeklyCurriculumOverviewInfo first = result.weeks().get(0);
            assertThat(first.weeklyCurriculumId()).isEqualTo(10L);
            assertThat(first.weekNo()).isEqualTo(1L);
            assertThat(first.title()).isEqualTo("1주차");
            assertThat(first.isExtra()).isFalse();
            assertThat(first.startsAt()).isEqualTo(WEEK1_START);
            assertThat(first.endsAt()).isEqualTo(WEEK1_END);
        }

        @Test
        void 특정_주차_필터링_조회에_성공한다() {
            // given
            WeeklyCurriculum wc1 = WeeklyCurriculum.create(curriculum, 1L, false, "1주차", WEEK1_START, WEEK1_END);
            ReflectionTestUtils.setField(wc1, "id", 10L);

            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, 1L)).willReturn(List.of(wc1));

            // when
            CurriculumOverviewInfo result = sut.getCurriculumOverview(GISU_ID, PART, 1L);

            // then
            assertThat(result.weeks()).hasSize(1);
            assertThat(result.weeks().get(0).weekNo()).isEqualTo(1L);
        }

        @Test
        void 부록_주차_포함_조회에_성공한다() {
            // given
            WeeklyCurriculum wc1 = WeeklyCurriculum.create(curriculum, 1L, false, "1주차", WEEK1_START, WEEK1_END);
            WeeklyCurriculum extra = WeeklyCurriculum.create(curriculum, 1L, true, "1주차 부록", WEEK1_START, WEEK1_END);
            ReflectionTestUtils.setField(wc1, "id", 10L);
            ReflectionTestUtils.setField(extra, "id", 11L);

            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1, extra));

            // when
            CurriculumOverviewInfo result = sut.getCurriculumOverview(GISU_ID, PART, null);

            // then
            assertThat(result.weeks()).hasSize(2);
            assertThat(result.weeks().get(1).isExtra()).isTrue();
        }

        @Test
        void 커리큘럼이_없으면_예외가_발생한다() {
            // given
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART))
                .willThrow(new CurriculumDomainException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> sut.getCurriculumOverview(GISU_ID, PART, null))
                .isInstanceOf(CurriculumDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
        }

        @Test
        void 주차가_없으면_빈_목록을_반환한다() {
            // given
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of());

            // when
            CurriculumOverviewInfo result = sut.getCurriculumOverview(GISU_ID, PART, null);

            // then
            assertThat(result.weeks()).isEmpty();
        }
    }

    // ===== getMyProgressV2 =====

    @Nested
    @DisplayName("내 커리큘럼 진행 상황 조회")
    class GetMyProgressV2 {

        private ChallengerInfo challengerInfo;
        private WeeklyCurriculum wc1;

        @BeforeEach
        void setUp() {
            given(getGisuUseCase.getById(GISU_ID)).willReturn(
                new GisuInfo(GISU_ID, GISU_ID, Instant.EPOCH, Instant.MAX, true));
            challengerInfo = ChallengerInfo.builder()
                .challengerId(50L)
                .memberId(MEMBER_ID)
                .gisuId(GISU_ID)
                .part(PART)
                .challengerPoints(List.of())
                .totalPoints(0.0)
                .challengerStatus(ChallengerStatus.ACTIVE)
                .build();

            wc1 = WeeklyCurriculum.create(curriculum, 1L, false, "1주차", WEEK1_START, WEEK1_END);
            ReflectionTestUtils.setField(wc1, "id", 10L);
        }

        @Test
        void 진행_상황_조회에_성공한다() {
            // given
            OriginalWorkbook wb = OriginalWorkbook.createAsReady(wc1, "1주차 워크북", "설명", null, null, OriginalWorkbookType.MAIN);
            ReflectionTestUtils.setField(wb, "id", 200L);

            OriginalWorkbookMission mission = Mockito.mock(OriginalWorkbookMission.class);
            given(mission.getId()).willReturn(300L);
            given(mission.getTitle()).willReturn("미션 1");
            given(mission.getDescription()).willReturn("미션 설명");
            given(mission.getMissionType()).willReturn(MissionType.LINK);
            given(mission.isNecessary()).willReturn(true);

            given(mission.getOriginalWorkbook()).willReturn(wb);

            given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(challengerInfo);
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1));
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L))).willReturn(List.of(wb));
            given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(200L))).willReturn(List.of(mission));
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(MEMBER_ID, List.of(200L)))
                .willReturn(List.of());

            // when
            MyCurriculumInfo result = sut.getMyProgress(MEMBER_ID, GISU_ID);

            // then
            assertThat(result.curriculumId()).isEqualTo(100L);
            assertThat(result.title()).isEqualTo("9기 스프링부트");
            assertThat(result.weeks()).hasSize(1);

            MyCurriculumInfo.MyWeeklyCurriculumInfo week = result.weeks().get(0);
            assertThat(week.weeklyCurriculumId()).isEqualTo(10L);
            assertThat(week.weekNo()).isEqualTo(1L);
            assertThat(week.releasedOriginalWorkbooks()).hasSize(1);

            MyCurriculumInfo.MyOriginalWorkbookInfo workbook = week.releasedOriginalWorkbooks().get(0);
            assertThat(workbook.originalWorkbookId()).isEqualTo(200L);
            assertThat(workbook.title()).isEqualTo("1주차 워크북");
            assertThat(workbook.missions()).hasSize(1);
            assertThat(workbook.missions().get(0).missionType()).isEqualTo(MissionType.LINK);
            assertThat(workbook.missions().get(0).isNecessary()).isTrue();
        }

        @Test
        void 챌린저_워크북이_있으면_isDeployedToMember가_true이다() {
            // given
            OriginalWorkbook wb = OriginalWorkbook.createAsReady(wc1, "워크북", null, null, null, OriginalWorkbookType.MAIN);
            ReflectionTestUtils.setField(wb, "id", 200L);

            ChallengerWorkbook cw = Mockito.mock(ChallengerWorkbook.class);
            given(cw.getId()).willReturn(999L);

            given(cw.getOriginalWorkbook()).willReturn(wb);

            given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(challengerInfo);
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1));
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L))).willReturn(List.of(wb));
            given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(200L))).willReturn(List.of());
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(MEMBER_ID, List.of(200L)))
                .willReturn(List.of(cw));
            given(loadMissionSubmissionPort.listActiveByChallengerWorkbookIdIn(List.of(999L))).willReturn(List.of());

            // when
            MyCurriculumInfo result = sut.getMyProgress(MEMBER_ID, GISU_ID);

            // then
            MyCurriculumInfo.MyOriginalWorkbookInfo workbook = result.weeks().get(0).releasedOriginalWorkbooks().get(0);
            assertThat(workbook.isDeployedToMember()).isTrue();
            assertThat(workbook.challengerWorkbookId()).isEqualTo(Optional.of(999L));
        }

        @Test
        void 챌린저_워크북이_없으면_isDeployedToMember가_false이다() {
            // given
            OriginalWorkbook wb = OriginalWorkbook.createAsReady(wc1, "워크북", null, null, null, OriginalWorkbookType.MAIN);
            ReflectionTestUtils.setField(wb, "id", 200L);

            given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(challengerInfo);
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1));
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L))).willReturn(List.of(wb));
            given(loadOriginalWorkbookMissionPort.findByOriginalWorkbookIdIn(List.of(200L))).willReturn(List.of());
            given(loadChallengerWorkbookPort.listByMemberIdAndOriginalWorkbookIdIn(MEMBER_ID, List.of(200L)))
                .willReturn(List.of());

            // when
            MyCurriculumInfo result = sut.getMyProgress(MEMBER_ID, GISU_ID);

            // then
            MyCurriculumInfo.MyOriginalWorkbookInfo workbook = result.weeks().get(0).releasedOriginalWorkbooks().get(0);
            assertThat(workbook.isDeployedToMember()).isFalse();
            assertThat(workbook.challengerWorkbookId()).isEmpty();
        }

        @Test
        void 배포된_워크북이_없으면_빈_목록을_반환한다() {
            // given
            given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(challengerInfo);
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1));
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L))).willReturn(List.of());

            // when
            MyCurriculumInfo result = sut.getMyProgress(MEMBER_ID, GISU_ID);

            // then
            assertThat(result.weeks().get(0).releasedOriginalWorkbooks()).isEmpty();
        }

        @Test
        void 주차_없이_전체_조회에_성공한다() {
            // given
            WeeklyCurriculum wc2 = WeeklyCurriculum.create(curriculum, 2L, false, "2주차", WEEK2_START, WEEK2_END);
            ReflectionTestUtils.setField(wc2, "id", 20L);

            given(getChallengerUseCase.getByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(challengerInfo);
            given(loadCurriculumPort.getByGisuIdAndPart(GISU_ID, PART)).willReturn(projection);
            given(loadWeeklyCurriculumPort.findByCurriculumId(100L, null)).willReturn(List.of(wc1, wc2));
            given(loadOriginalWorkbookPort.findReleasedByWeeklyCurriculumIdIn(List.of(10L, 20L))).willReturn(List.of());

            // when
            MyCurriculumInfo result = sut.getMyProgress(MEMBER_ID, GISU_ID);

            // then
            assertThat(result.weeks()).hasSize(2);
            assertThat(result.weeks().get(0).weekNo()).isEqualTo(1L);
            assertThat(result.weeks().get(1).weekNo()).isEqualTo(2L);
        }
    }
}
