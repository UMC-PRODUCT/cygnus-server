package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolChapterNameInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;
import com.umc.product.recruiting.application.port.out.LoadRecruitingEvaluationStatisticsPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingEvaluationStatisticsRow;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;

@ExtendWith(MockitoExtension.class)
class RecruitingEvaluationStatisticsQueryServiceTest {

    private static final Long MEMBER_ID = 99L;
    private static final Long GISU_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-07-04T02:48:00Z");

    @Mock
    LoadRecruitingEvaluationStatisticsPort loadStatisticsPort;

    @Mock
    GetSchoolUseCase getSchoolUseCase;

    @Mock
    CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;

    @Mock
    Clock clock;

    @InjectMocks
    RecruitingEvaluationStatisticsQueryService sut;

    @Test
    @DisplayName("기수_운영진이_아니면_평가_현황을_조회할_수_없다")
    void denyWithoutAnyStaffRole() {
        given(checkChallengerAuthorityUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(checkChallengerAuthorityUseCase.hasAnyRoleTypeInGisu(MEMBER_ID, GISU_ID, ChallengerRoleType.values()))
            .willReturn(false);

        assertThatThrownBy(() -> sut.getEvaluationStatistics(query()))
            .isInstanceOf(RecruitingDomainException.class);
        then(loadStatisticsPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("SUPER_ADMIN은_기수_역할이_없어도_조회할_수_있다")
    void allowSuperAdminWithoutGisuRole() {
        given(checkChallengerAuthorityUseCase.isSuperAdmin(MEMBER_ID)).willReturn(true);
        given(clock.instant()).willReturn(NOW);
        given(loadStatisticsPort.listByGisuId(GISU_ID)).willReturn(List.of());
        given(getSchoolUseCase.getSchoolChapterNamesByGisuId(GISU_ID)).willReturn(List.of());

        RecruitingEvaluationStatisticsInfo info = sut.getEvaluationStatistics(query());

        assertThat(info.asOf()).isEqualTo(NOW);
        assertThat(info.applicantCount()).isZero();
        assertThat(info.evaluatedCount()).isZero();
    }

    @Test
    @DisplayName("분모는_DRAFT_CANCELLED_제외_분자는_판정_확정_상태만_집계한다")
    void aggregateByEvaluationDefinition() {
        givenStaffAccess();
        given(clock.instant()).willReturn(NOW);
        given(loadStatisticsPort.listByGisuId(GISU_ID)).willReturn(List.of(
            row(10L, ChallengerTrack.PLAN, RecruitingApplicationStatus.SUBMITTED, 3L),
            row(10L, ChallengerTrack.PLAN, RecruitingApplicationStatus.FINAL_PASSED, 2L),
            row(10L, ChallengerTrack.DESIGN, RecruitingApplicationStatus.DOCUMENT_FAILED, 1L),
            row(20L, ChallengerTrack.WEB_PRODUCT_ENGINEER, RecruitingApplicationStatus.FINAL_FAILED, 4L),
            row(20L, ChallengerTrack.MOBILE_PRODUCT_ENGINEER, RecruitingApplicationStatus.INTERVIEW_ASSIGNED, 5L)
        ));
        given(getSchoolUseCase.getSchoolChapterNamesByGisuId(GISU_ID)).willReturn(List.of(
            schoolName(10L, "가온", 1L, "한국대"),
            schoolName(20L, "나래", 2L, "중앙대")
        ));

        RecruitingEvaluationStatisticsInfo info = sut.getEvaluationStatistics(query());

        assertThat(info.applicantCount()).isEqualTo(15L);
        assertThat(info.evaluatedCount()).isEqualTo(7L);

        assertThat(info.byTrack()).extracting(
                RecruitingTrackEvaluationCountInfo::track,
                RecruitingTrackEvaluationCountInfo::applicantCount,
                RecruitingTrackEvaluationCountInfo::evaluatedCount)
            .containsExactly(
                tuple(ChallengerTrack.PLAN, 5L, 2L),
                tuple(ChallengerTrack.DESIGN, 1L, 1L),
                tuple(ChallengerTrack.WEB_PRODUCT_ENGINEER, 4L, 4L),
                tuple(ChallengerTrack.MOBILE_PRODUCT_ENGINEER, 5L, 0L)
            );
    }

    @Test
    @DisplayName("일부 트랙에만 지원서가 있어도 모든 트랙을 0건 포함해 반환한다")
    void includeZeroCountTracksWhenNoRowsExist() {
        givenStaffAccess();
        given(clock.instant()).willReturn(NOW);
        given(loadStatisticsPort.listByGisuId(GISU_ID)).willReturn(List.of(
            row(10L, ChallengerTrack.PLAN, RecruitingApplicationStatus.SUBMITTED, 3L)
        ));
        given(getSchoolUseCase.getSchoolChapterNamesByGisuId(GISU_ID)).willReturn(List.of(
            schoolName(10L, "가온", 1L, "한국대")
        ));

        RecruitingEvaluationStatisticsInfo info = sut.getEvaluationStatistics(query());

        assertThat(info.byTrack()).extracting(
                RecruitingTrackEvaluationCountInfo::track,
                RecruitingTrackEvaluationCountInfo::applicantCount,
                RecruitingTrackEvaluationCountInfo::evaluatedCount)
            .containsExactly(
                tuple(ChallengerTrack.PLAN, 3L, 0L),
                tuple(ChallengerTrack.DESIGN, 0L, 0L),
                tuple(ChallengerTrack.WEB_PRODUCT_ENGINEER, 0L, 0L),
                tuple(ChallengerTrack.MOBILE_PRODUCT_ENGINEER, 0L, 0L)
            );
    }

    @Test
    @DisplayName("지부는_가나다순_지부_내_학교도_가나다순으로_정렬하고_지원서_없는_학교도_0건으로_반환한다")
    void sortChaptersAndSchoolsIncludingEmptySchools() {
        givenStaffAccess();
        given(clock.instant()).willReturn(NOW);
        given(loadStatisticsPort.listByGisuId(GISU_ID)).willReturn(List.of(
            row(30L, ChallengerTrack.PLAN, RecruitingApplicationStatus.SUBMITTED, 2L)
        ));
        given(getSchoolUseCase.getSchoolChapterNamesByGisuId(GISU_ID)).willReturn(List.of(
            schoolName(30L, "나래", 2L, "홍익대"),
            schoolName(20L, "가온", 1L, "중앙대"),
            schoolName(10L, "가온", 1L, "건국대")
        ));

        RecruitingEvaluationStatisticsInfo info = sut.getEvaluationStatistics(query());

        assertThat(info.chapters()).extracting(RecruitingChapterEvaluationStatisticsInfo::chapterName)
            .containsExactly("가온", "나래");
        assertThat(info.chapters().get(0).schools())
            .extracting(RecruitingSchoolEvaluationStatisticsInfo::schoolName)
            .containsExactly("건국대", "중앙대");
        assertThat(info.chapters().get(0).schools())
            .allSatisfy(school -> {
                assertThat(school.applicantCount()).isZero();
                assertThat(school.evaluatedCount()).isZero();
            });
        assertThat(info.chapters().get(1).applicantCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("학교_목록에_없는_학교의_row는_전체_합계에서도_제외한다")
    void excludeRowsOfUnknownSchools() {
        givenStaffAccess();
        given(clock.instant()).willReturn(NOW);
        given(loadStatisticsPort.listByGisuId(GISU_ID)).willReturn(List.of(
            row(10L, ChallengerTrack.PLAN, RecruitingApplicationStatus.FINAL_PASSED, 3L),
            row(999L, ChallengerTrack.PLAN, RecruitingApplicationStatus.FINAL_PASSED, 7L)
        ));
        given(getSchoolUseCase.getSchoolChapterNamesByGisuId(GISU_ID)).willReturn(List.of(
            schoolName(10L, "가온", 1L, "한국대")
        ));

        RecruitingEvaluationStatisticsInfo info = sut.getEvaluationStatistics(query());

        assertThat(info.applicantCount()).isEqualTo(3L);
        assertThat(info.evaluatedCount()).isEqualTo(3L);
        assertThat(info.chapters().get(0).applicantCount()).isEqualTo(3L);
    }

    private void givenStaffAccess() {
        given(checkChallengerAuthorityUseCase.isSuperAdmin(MEMBER_ID)).willReturn(false);
        given(checkChallengerAuthorityUseCase.hasAnyRoleTypeInGisu(MEMBER_ID, GISU_ID, ChallengerRoleType.values()))
            .willReturn(true);
    }

    private RecruitingEvaluationStatisticsQuery query() {
        return RecruitingEvaluationStatisticsQuery.builder()
            .gisuId(GISU_ID)
            .requesterMemberId(MEMBER_ID)
            .build();
    }

    private RecruitingEvaluationStatisticsRow row(
        Long schoolId,
        ChallengerTrack track,
        RecruitingApplicationStatus status,
        Long count
    ) {
        return new RecruitingEvaluationStatisticsRow(schoolId, track, status, count);
    }

    private SchoolDetailInfo school(Long chapterId, String chapterName, Long schoolId, String schoolName) {
        return new SchoolDetailInfo(chapterId, chapterName, schoolName, schoolId,
            null, null, List.of(), true, NOW, NOW);
    }

    private SchoolChapterNameInfo schoolName(Long chapterId, String chapterName, Long schoolId, String schoolName) {
        return new SchoolChapterNameInfo(chapterId, chapterName, schoolName, schoolId);
    }
}
