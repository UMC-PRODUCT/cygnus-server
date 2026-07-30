package com.umc.product.recruiting.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.application.port.out.LoadRecruitingApplicationPort;
import com.umc.product.recruiting.application.port.out.LoadRecruitingDecisionHistoryPort;
import com.umc.product.recruiting.application.port.out.dto.RecruitingApplicationSummaryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistoryRow;
import com.umc.product.recruiting.application.port.out.dto.RecruitingDecisionHistorySearchCondition;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;
import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

@ExtendWith(MockitoExtension.class)
class RecruitingDecisionHistoryQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
    private static final Long GISU_ID = 1L;
    private static final Long REQUESTER_ID = 40L;
    private static final Long CHROMIUM_CHAPTER_ID = 100L;
    private static final Long SELENIUM_CHAPTER_ID = 200L;
    private static final Long HANYANG_SCHOOL_ID = 10L;
    private static final Long SOONGSIL_SCHOOL_ID = 20L;

    @Mock
    LoadRecruitingDecisionHistoryPort loadDecisionHistoryPort;
    @Mock
    LoadRecruitingApplicationPort loadApplicationPort;
    @Mock
    GetSchoolUseCase getSchoolUseCase;
    @Mock
    CheckChallengerAuthorityUseCase checkChallengerAuthorityUseCase;

    RecruitingDecisionHistoryQueryService sut;

    @BeforeEach
    void setUp() {
        sut = new RecruitingDecisionHistoryQueryService(
            loadDecisionHistoryPort,
            loadApplicationPort,
            getSchoolUseCase,
            checkChallengerAuthorityUseCase,
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Nested
    @DisplayName("접근 권한")
    class Access {

        @Test
        @DisplayName("중앙운영사무국 구성원은 조회할 수 있다")
        void centralMemberCanSearch() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(false);
            given(checkChallengerAuthorityUseCase.isCentralMemberInGisu(REQUESTER_ID, GISU_ID)).willReturn(true);
            givenSchools();
            givenEmptySearch();
            givenNoDecisionTargets();

            RecruitingDecisionHistoryPageInfo result = sut.search(defaultQuery().build());

            assertThat(result.asOf()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("SUPER_ADMIN은 조회할 수 있다")
        void superAdminCanSearch() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(true);
            givenSchools();
            givenEmptySearch();
            givenNoDecisionTargets();

            sut.search(defaultQuery().build());

            then(checkChallengerAuthorityUseCase).should().isSuperAdmin(REQUESTER_ID);
        }

        @Test
        @DisplayName("지부장·학교 운영진 등 중앙 구성원이 아니면 403으로 거부한다")
        void nonCentralMemberIsDenied() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(false);
            given(checkChallengerAuthorityUseCase.isCentralMemberInGisu(REQUESTER_ID, GISU_ID)).willReturn(false);

            assertThatThrownBy(() -> sut.search(defaultQuery().build()))
                .isInstanceOf(RecruitingDomainException.class)
                .extracting("baseCode")
                .isEqualTo(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_ACCESS_DENIED);

            then(loadDecisionHistoryPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("CSV 다운로드도 같은 권한 규칙으로 거부한다")
        void csvExportUsesSameAccessRule() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(false);
            given(checkChallengerAuthorityUseCase.isCentralMemberInGisu(REQUESTER_ID, GISU_ID)).willReturn(false);

            assertThatThrownBy(() -> sut.exportCsv(defaultQuery().build()))
                .isInstanceOf(RecruitingDomainException.class)
                .extracting("baseCode")
                .isEqualTo(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("검색 조건 매핑")
    class ConditionMapping {

        @BeforeEach
        void allowAccess() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(true);
        }

        @Test
        @DisplayName("평가 결과 필터는 판정 상태 집합으로 변환한다")
        void resultsAreMappedToDecisionStatuses() {
            givenSchools();
            givenEmptySearch();
            givenNoDecisionTargets();

            sut.search(defaultQuery()
                .results(Set.of(RecruitingDecisionResult.FAILED))
                .build());

            RecruitingDecisionHistorySearchCondition condition = capturedCondition();
            assertThat(condition.decisionStatuses()).containsExactlyInAnyOrder(
                RecruitingApplicationStatus.DOCUMENT_FAILED,
                RecruitingApplicationStatus.FINAL_FAILED
            );
        }

        @Test
        @DisplayName("지부 필터는 해당 지부 학교들로 검색 범위를 좁힌다")
        void chapterFilterNarrowsSchoolScope() {
            givenSchools();
            givenEmptySearch();
            givenNoDecisionTargets();

            sut.search(defaultQuery().chapterId(CHROMIUM_CHAPTER_ID).build());

            assertThat(capturedCondition().schoolIds()).containsExactly(HANYANG_SCHOOL_ID);
        }

        @Test
        @DisplayName("지부와 학교가 서로 다른 지부 조합이면 빈 결과를 반환한다")
        void mismatchedChapterAndSchoolReturnsEmpty() {
            givenSchools();

            RecruitingDecisionHistoryPageInfo result = sut.search(defaultQuery()
                .chapterId(SELENIUM_CHAPTER_ID)
                .schoolId(HANYANG_SCHOOL_ID)
                .build());

            assertThat(result.page().getContent()).isEmpty();
            then(loadDecisionHistoryPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("담당자 이름 검색은 판정 시점 스냅샷 조건으로 전달한다")
        void deciderNameSearchUsesSnapshotCondition() {
            givenSchools();
            givenEmptySearch();
            givenNoDecisionTargets();

            sut.search(defaultQuery().searchName("방토").build());

            RecruitingDecisionHistorySearchCondition condition = capturedCondition();
            assertThat(condition.searchName()).isEqualTo("방토");
        }
    }

    @Nested
    @DisplayName("상태 뱃지")
    class ProgressBadge {

        @BeforeEach
        void allowAccess() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(true);
            givenSchools();
            givenEmptySearch();
        }

        @Test
        @DisplayName("판정 완료가 없으면 최종 평가 전이다")
        void beforeEvaluationWhenNothingDecided() {
            givenDecisionTargets(RecruitingApplicationStatus.SUBMITTED, RecruitingApplicationStatus.INTERVIEW_SKIPPED);

            assertThat(sut.search(defaultQuery().build()).progressStatus())
                .isEqualTo(RecruitingEvaluationProgressStatus.BEFORE_EVALUATION);
        }

        @Test
        @DisplayName("일부만 판정 완료면 평가 진행 중이다")
        void inProgressWhenPartiallyDecided() {
            givenDecisionTargets(RecruitingApplicationStatus.FINAL_PASSED, RecruitingApplicationStatus.SUBMITTED);

            assertThat(sut.search(defaultQuery().build()).progressStatus())
                .isEqualTo(RecruitingEvaluationProgressStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("판정 대상 전원이 판정 완료면 최종 평가 완료다")
        void completedWhenAllDecided() {
            givenDecisionTargets(
                RecruitingApplicationStatus.FINAL_PASSED,
                RecruitingApplicationStatus.FINAL_FAILED,
                RecruitingApplicationStatus.DOCUMENT_FAILED
            );

            assertThat(sut.search(defaultQuery().build()).progressStatus())
                .isEqualTo(RecruitingEvaluationProgressStatus.COMPLETED);
        }

        @Test
        @DisplayName("판정 대상이 없으면 최종 평가 전이다")
        void beforeEvaluationWhenNoTargets() {
            givenNoDecisionTargets();

            assertThat(sut.search(defaultQuery().build()).progressStatus())
                .isEqualTo(RecruitingEvaluationProgressStatus.BEFORE_EVALUATION);
        }
    }

    @Nested
    @DisplayName("행 조립")
    class RowAssembly {

        @BeforeEach
        void allowAccess() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(true);
            givenSchools();
            givenNoDecisionTargets();
        }

        @Test
        @DisplayName("담당자 정보는 판정 시점 스냅샷으로 노출한다")
        void deciderInfoUsesDecisionTimeSnapshot() {
            givenSearchReturns(row(1L, HANYANG_SCHOOL_ID, ChallengerRoleType.SCHOOL_PRESIDENT, 70L));

            RecruitingDecisionHistoryInfo info = sut.search(defaultQuery().build()).page().getContent().get(0);

            assertThat(info.decider().schoolId()).isEqualTo(30L);
            assertThat(info.decider().schoolName()).isEqualTo("판정 당시 학교");
            assertThat(info.decider().chapterId()).isEqualTo(300L);
            assertThat(info.decider().chapterName()).isEqualTo("판정 당시 지부");
            assertThat(info.decider().name()).isEqualTo("판정 당시 이름");
            assertThat(info.decider().nickname()).isEqualTo("판정닉");
            assertThat(info.result()).isEqualTo(RecruitingDecisionResult.PASSED);
        }

        @Test
        @DisplayName("중앙 직위 담당자는 지부·학교 소속 없이 노출한다")
        void centralTierDeciderHasNoSchool() {
            givenSearchReturns(row(1L, HANYANG_SCHOOL_ID, ChallengerRoleType.CENTRAL_PRESIDENT, 70L));

            RecruitingDecisionHistoryInfo info = sut.search(defaultQuery().build()).page().getContent().get(0);

            assertThat(info.decider().schoolId()).isNull();
            assertThat(info.decider().chapterName()).isNull();
            assertThat(info.decider().roleType()).isEqualTo(ChallengerRoleType.CENTRAL_PRESIDENT);
        }

        @Test
        @DisplayName("담당자 탈퇴 후에도 판정 시점 이름과 닉네임을 노출한다")
        void withdrawnDeciderIsExposedWithSnapshotName() {
            givenSearchReturns(row(1L, HANYANG_SCHOOL_ID, ChallengerRoleType.SCHOOL_PRESIDENT, 70L));

            RecruitingDecisionHistoryInfo info = sut.search(defaultQuery().build()).page().getContent().get(0);

            assertThat(info.decider().memberId()).isEqualTo(70L);
            assertThat(info.decider().name()).isEqualTo("판정 당시 이름");
            assertThat(info.decider().nickname()).isEqualTo("판정닉");
        }
    }

    @Nested
    @DisplayName("CSV")
    class Csv {

        @BeforeEach
        void allowAccess() {
            given(checkChallengerAuthorityUseCase.isSuperAdmin(REQUESTER_ID)).willReturn(true);
            givenSchools();
        }

        @Test
        @DisplayName("CSV는 마스킹된 이메일과 닉네임만 포함하고 실명은 포함하지 않는다")
        void csvExcludesRawEmailAndRealNames() {
            given(loadDecisionHistoryPort.searchRows(any(), any())).willReturn(
                new PageImpl<>(List.of(row(1L, HANYANG_SCHOOL_ID, ChallengerRoleType.SCHOOL_PRESIDENT, 70L)))
            );

            String csv = new String(sut.exportCsv(defaultQuery().build()), StandardCharsets.UTF_8);

            assertThat(csv).startsWith("decidedAt,decisionStatus,result,");
            assertThat(csv).contains("판정닉");
            assertThat(csv).doesNotContain("판정 당시 이름");
            assertThat(csv).doesNotContain("applicant@example.com");
            assertThat(csv).doesNotContain("홍길동");
        }

        @Test
        @DisplayName("다운로드 대상이 상한(50,000건)을 초과하면 거부한다")
        void csvRejectsWhenExportExceedsMaxRows() {
            // 상한 초과를 재현하기 위해 MAX_EXPORT_ROWS(50,000) + 1 건을 반환한다.
            given(loadDecisionHistoryPort.searchRows(any(), any())).willReturn(new PageImpl<>(
                Collections.nCopies(50_001, row(1L, HANYANG_SCHOOL_ID, ChallengerRoleType.SCHOOL_PRESIDENT, 70L))
            ));

            assertThatThrownBy(() -> sut.exportCsv(defaultQuery().build()))
                .isInstanceOf(RecruitingDomainException.class)
                .extracting("baseCode")
                .isEqualTo(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_EXPORT_TOO_LARGE);
        }
    }

    @Nested
    @DisplayName("페이지 크기 검증")
    class PageSize {

        @Test
        @DisplayName("size가 100을 초과하면 조회 요청을 거부한다")
        void rejectsPageSizeOverLimit() {
            assertThatThrownBy(() -> defaultQuery().pageable(PageRequest.of(0, 101)).build())
                .isInstanceOf(RecruitingDomainException.class)
                .extracting("baseCode")
                .isEqualTo(RecruitingErrorCode.RECRUITING_DECISION_HISTORY_INVALID_PAGE_SIZE);
        }

        @Test
        @DisplayName("size가 100 이하이면 허용한다")
        void allowsPageSizeWithinLimit() {
            assertThat(defaultQuery().pageable(PageRequest.of(0, 100)).build().pageable().getPageSize())
                .isEqualTo(100);
        }
    }

    private RecruitingDecisionHistorySearchQuery.RecruitingDecisionHistorySearchQueryBuilder defaultQuery() {
        return RecruitingDecisionHistorySearchQuery.builder()
            .gisuId(GISU_ID)
            .requesterMemberId(REQUESTER_ID)
            .pageable(PageRequest.of(0, 20));
    }

    private RecruitingDecisionHistorySearchCondition capturedCondition() {
        ArgumentCaptor<RecruitingDecisionHistorySearchCondition> captor =
            ArgumentCaptor.forClass(RecruitingDecisionHistorySearchCondition.class);
        then(loadDecisionHistoryPort).should().searchRows(captor.capture(), any());
        return captor.getValue();
    }

    private void givenSchools() {
        given(getSchoolUseCase.getSchoolListByGisuId(GISU_ID)).willReturn(List.of(
            school(CHROMIUM_CHAPTER_ID, "Chromium", HANYANG_SCHOOL_ID, "한양대 ERICA"),
            school(SELENIUM_CHAPTER_ID, "Selenium", SOONGSIL_SCHOOL_ID, "숭실대")
        ));
    }

    private void givenEmptySearch() {
        given(loadDecisionHistoryPort.searchRows(any(), any())).willReturn(new PageImpl<>(List.of()));
    }

    private void givenSearchReturns(RecruitingDecisionHistoryRow row) {
        given(loadDecisionHistoryPort.searchRows(any(), any())).willReturn(new PageImpl<>(List.of(row)));
    }

    private void givenNoDecisionTargets() {
        given(loadApplicationPort.searchSummaryRows(eq(GISU_ID), anySet(), eq(null), anySet())).willReturn(List.of());
    }

    private void givenDecisionTargets(RecruitingApplicationStatus... statuses) {
        List<RecruitingApplicationSummaryRow> rows = List.of(statuses).stream()
            .map(status -> new RecruitingApplicationSummaryRow(
                1L, GISU_ID, HANYANG_SCHOOL_ID, 1L, "본모집", null, 1, 1L, 1L,
                1L, "지원자", "applicant@example.com",
                ChallengerTrack.WEB_PRODUCT_ENGINEER, null, null,
                status, null, NOW
            ))
            .toList();
        given(loadApplicationPort.searchSummaryRows(eq(GISU_ID), anySet(), eq(null), anySet())).willReturn(rows);
    }

    private RecruitingDecisionHistoryRow row(
        Long historyId,
        Long schoolId,
        ChallengerRoleType deciderRoleType,
        Long deciderMemberId
    ) {
        return new RecruitingDecisionHistoryRow(
            historyId,
            900L,
            schoolId,
            "홍길동",
            "applicant@example.com",
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            null,
            ChallengerTrack.WEB_PRODUCT_ENGINEER,
            RecruitingApplicationStatus.FINAL_PASSED,
            NOW,
            deciderMemberId,
            deciderRoleType,
            deciderRoleType.isAtLeastSchoolCore() ? 300L : null,
            deciderRoleType.isAtLeastSchoolCore() ? "판정 당시 지부" : null,
            deciderRoleType.isAtLeastSchoolCore() ? 30L : null,
            deciderRoleType.isAtLeastSchoolCore() ? "판정 당시 학교" : null,
            "판정 당시 이름",
            "판정닉"
        );
    }

    private SchoolDetailInfo school(Long chapterId, String chapterName, Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            chapterId, chapterName, schoolName, schoolId, null, null, List.of(), true, NOW, NOW
        );
    }

}
