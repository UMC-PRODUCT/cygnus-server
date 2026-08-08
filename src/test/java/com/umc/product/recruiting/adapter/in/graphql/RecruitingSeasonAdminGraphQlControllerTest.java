package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingEvaluationStatisticsUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingChapterEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistoryPageInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingEvaluationStatisticsQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPartStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundGroupSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolEvaluationStatisticsInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSchoolStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingTrackEvaluationCountInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluationProgressStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;

@GraphQlTest(RecruitingAdminGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingSeasonAdminGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    SearchRecruitingDecisionHistoryUseCase searchDecisionHistoryUseCase;
    @MockitoBean
    GetRecruitingEvaluationStatisticsUseCase getEvaluationStatisticsUseCase;
    @MockitoBean
    GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    @MockitoBean
    SearchRecruitingSeasonUseCase searchSeasonUseCase;
    @MockitoBean
    SearchRecruitingRoundUseCase searchRoundUseCase;
    @MockitoBean
    SearchRecruitingRoundGroupUseCase searchRoundGroupUseCase;
    @MockitoBean
    CheckRecruitingRoundTitleUseCase checkRoundTitleUseCase;
    @MockitoBean
    CloneRecruitingRoundUseCase cloneRoundUseCase;
    @MockitoBean
    DeleteRecruitingRoundUseCase deleteRoundUseCase;
    @MockitoBean
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonUseCase updateSeasonUseCase;
    @MockitoBean
    ReplaceRecruitingSeasonTrackQuotasUseCase replaceQuotasUseCase;
    @MockitoBean
    CreateRecruitingRoundUseCase createRoundUseCase;
    @MockitoBean
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    @MockitoBean
    UpdateRecruitingRoundUseCase updateRoundUseCase;
    @MockitoBean
    GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;
    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;
    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(40L), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GraphQL 시즌 생성은 0명을 포함한 쿼터를 command로 전달한다")
    void createSeasonWithQuotas() {
        given(createSeasonUseCase.createSeason(any())).willReturn(10L);

        graphQlTester.document("""
                mutation {
                  createRecruitingSeason(input: {
                    gisuId: 11,
                    schoolId: 22,
                    quotas: [
                      {track: PLAN, targetCount: 0},
                      {track: DESIGN, targetCount: 4}
                    ]
                  }) { id }
                }
                """)
            .execute()
            .path("createRecruitingSeason.id")
            .entity(String.class)
            .isEqualTo("10");

        ArgumentCaptor<CreateRecruitingSeasonCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingSeasonCommand.class);
        then(createSeasonUseCase).should().createSeason(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
        assertThat(captor.getValue().quotas()).hasSize(2);
        assertThat(captor.getValue().quotas().getFirst().targetCount()).isZero();
    }

    @Test
    @DisplayName("GraphQL 시즌 설정 조회는 availability Form과 SCHEDULE 질문 ID를 함께 반환한다")
    void getSeasonConfigurationIncludesAvailabilityQuestionId() {
        given(getSeasonConfigurationUseCase.getBySeasonId(10L)).willReturn(
            new RecruitingSeasonConfigurationInfo(10L, 11L, 22L, "메모", List.of(), List.of(roundConfiguration()))
        );

        graphQlTester.document("""
                query {
                  recruitingSeasonConfiguration(seasonId: 10) {
                    rounds { availabilityFormId availabilityScheduleQuestionId }
                  }
                }
                """)
            .execute()
            .path("recruitingSeasonConfiguration.rounds[0].availabilityFormId")
            .entity(String.class)
            .isEqualTo("100")
            .path("recruitingSeasonConfiguration.rounds[0].availabilityScheduleQuestionId")
            .entity(String.class)
            .isEqualTo("200");
    }

    @Test
    @DisplayName("GraphQL 쿼터 교체는 seasonId와 트랙 설정을 command로 전달한다")
    void replaceSeasonQuotas() {
        graphQlTester.document("""
                mutation {
                  replaceRecruitingSeasonTrackQuotas(
                    seasonId: 10,
                    input: {quotas: [{track: WEB_PRODUCT_ENGINEER, targetCount: 3}]}
                  )
                }
                """)
            .execute()
            .path("replaceRecruitingSeasonTrackQuotas")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<ReplaceRecruitingSeasonTrackQuotasCommand> captor =
            ArgumentCaptor.forClass(ReplaceRecruitingSeasonTrackQuotasCommand.class);
        then(replaceQuotasUseCase).should().replaceQuotas(captor.capture());
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().quotas().getFirst().track())
            .isEqualTo(ChallengerTrack.WEB_PRODUCT_ENGINEER);
    }

    @Test
    @DisplayName("GraphQL 상태 요약은 CurrentMember와 요청 기수를 public UseCase에 전달한다")
    void statusSummaryBindsCurrentMemberAndRequestedGisu() {
        List<RecruitingPartStatusSummaryInfo> parts = List.of(
            new RecruitingPartStatusSummaryInfo(
                ChallengerTrack.PLAN, 3L, Map.of(RecruitingApplicationStatus.SUBMITTED, 3L)),
            new RecruitingPartStatusSummaryInfo(
                ChallengerTrack.DESIGN, 0L, Map.of())
        );
        RecruitingRoundStatusSummaryInfo round = new RecruitingRoundStatusSummaryInfo(
            31L, "1차 서류", RecruitingRoundType.REGULAR, 1, 3L,
            Map.of(RecruitingApplicationStatus.SUBMITTED, 3L), parts
        );
        given(getApplicationQueryUseCase.getStatusSummary(any()))
            .willReturn(new RecruitingStatusSummaryInfo(
                3L,
                Map.of(RecruitingApplicationStatus.SUBMITTED, 3L),
                parts,
                List.of(new RecruitingSchoolStatusSummaryInfo(
                    22L, "테스트대학교", 3L, "중앙", 3L,
                    Map.of(RecruitingApplicationStatus.SUBMITTED, 3L), parts, List.of(round)
                ))
            ));

        graphQlTester.document("""
                query {
                  recruitingStatusSummary(input: {
                    gisuId: 11,
                    schoolIds: [22, 23],
                    roundIds: [31],
                    schoolName: "테스트"
                  }) {
                    totalCount
                    countByStatus { status count }
                    parts { part totalCount countByStatus { status count } }
                    schools {
                      schoolId schoolName totalCount
                      parts { part totalCount }
                      rounds { roundId parts { part totalCount } }
                    }
                  }
                }
                """)
            .execute()
            .path("recruitingStatusSummary.totalCount").entity(Long.class).isEqualTo(3L)
            .path("recruitingStatusSummary.parts[0].part").entity(String.class).isEqualTo("PLAN")
            .path("recruitingStatusSummary.parts[0].totalCount").entity(Long.class).isEqualTo(3L)
            .path("recruitingStatusSummary.parts[0].countByStatus[0].status").entity(String.class).isEqualTo("SUBMITTED")
            .path("recruitingStatusSummary.parts[0].countByStatus[0].count").entity(Long.class).isEqualTo(3L)
            .path("recruitingStatusSummary.parts[1].part").entity(String.class).isEqualTo("DESIGN")
            .path("recruitingStatusSummary.parts[1].totalCount").entity(Long.class).isEqualTo(0L)
            .path("recruitingStatusSummary.schools[0].parts[0].part").entity(String.class).isEqualTo("PLAN")
            .path("recruitingStatusSummary.schools[0].rounds[0].parts[0].part").entity(String.class).isEqualTo("PLAN");

        ArgumentCaptor<RecruitingStatusSummaryQuery> captor =
            ArgumentCaptor.forClass(RecruitingStatusSummaryQuery.class);
        then(getApplicationQueryUseCase).should().getStatusSummary(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().schoolIds()).containsExactlyInAnyOrder(22L, 23L);
        assertThat(captor.getValue().roundIds()).containsExactly(31L);
        assertThat(captor.getValue().schoolName()).isEqualTo("테스트");
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 평가 현황은 CurrentMember와 요청 기수를 UseCase에 전달한다")
    void evaluationStatisticsBindsCurrentMemberAndRequestedGisu() {
        List<RecruitingTrackEvaluationCountInfo> byTrack = List.of(
            new RecruitingTrackEvaluationCountInfo(ChallengerTrack.PLAN, 5L, 2L)
        );
        given(getEvaluationStatisticsUseCase.getEvaluationStatistics(any()))
            .willReturn(new RecruitingEvaluationStatisticsInfo(
                Instant.parse("2026-07-04T02:48:00Z"), 5L, 2L, byTrack,
                List.of(new RecruitingChapterEvaluationStatisticsInfo(7L, "중앙", 5L, 2L, byTrack,
                    List.of(new RecruitingSchoolEvaluationStatisticsInfo(22L, "테스트대학교", 5L, 2L, byTrack))
                ))
            ));

        graphQlTester.document("""
                query {
                  recruitingEvaluationStatistics(input: { gisuId: 11 }) {
                    asOf
                    applicantCount
                    evaluatedCount
                    byTrack { track applicantCount evaluatedCount }
                    chapters {
                      chapterName
                      applicantCount
                      schools { schoolName applicantCount evaluatedCount }
                    }
                  }
                }
                """)
            .execute()
            .path("recruitingEvaluationStatistics.applicantCount")
            .entity(Long.class)
            .isEqualTo(5L)
            .path("recruitingEvaluationStatistics.chapters[0].schools[0].schoolName")
            .entity(String.class)
            .isEqualTo("테스트대학교");

        ArgumentCaptor<RecruitingEvaluationStatisticsQuery> captor =
            ArgumentCaptor.forClass(RecruitingEvaluationStatisticsQuery.class);
        then(getEvaluationStatisticsUseCase).should().getEvaluationStatistics(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 평가 이력은 필터·정렬 조건과 CurrentMember를 query로 전달한다")
    void decisionHistoriesBindsFiltersAndCurrentMember() {
        given(searchDecisionHistoryUseCase.search(any())).willReturn(new RecruitingDecisionHistoryPageInfo(
            Instant.parse("2026-07-04T02:48:00Z"),
            RecruitingEvaluationProgressStatus.IN_PROGRESS,
            new PageImpl<>(
                List.of(RecruitingDecisionHistoryInfo.builder()
                    .decisionHistoryId(1L)
                    .applicationId(900L)
                    .decidedAt(Instant.parse("2026-07-04T02:48:00Z"))
                    .decisionStatus(RecruitingApplicationStatus.FINAL_PASSED)
                    .result(RecruitingDecisionResult.PASSED)
                    .applicant(RecruitingDecisionHistoryInfo.ApplicantInfo.builder()
                        .chapterId(5L).chapterName("Selenium").schoolId(22L).schoolName("한양대 ERICA")
                        .name("박유엠").firstChoice(ChallengerTrack.WEB_PRODUCT_ENGINEER).build())
                    .decider(RecruitingDecisionHistoryInfo.DeciderInfo.builder()
                        .memberId(70L).roleType(ChallengerRoleType.SCHOOL_PRESIDENT)
                        .name("이예원").nickname("이방토").build())
                    .build()),
                PageRequest.of(0, 20),
                1L
            )
        ));

        graphQlTester.document("""
                query {
                  recruitingDecisionHistories(input: {
                    gisuId: 11,
                    chapterIds: [5, 6],
                    schoolIds: [22, 23],
                    results: [PASSED],
                    sort: OLDEST,
                    groupByDecider: true
                  }) {
                    progressStatus
                    totalElements
                    content { applicant { name } decider { roleType nickname } }
                  }
                }
                """)
            .execute()
            .path("recruitingDecisionHistories.totalElements")
            .entity(Long.class)
            .isEqualTo(1L);

        ArgumentCaptor<RecruitingDecisionHistorySearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingDecisionHistorySearchQuery.class);
        then(searchDecisionHistoryUseCase).should().search(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().chapterIds()).containsExactlyInAnyOrder(5L, 6L);
        assertThat(captor.getValue().schoolIds()).containsExactlyInAnyOrder(22L, 23L);
        assertThat(captor.getValue().results()).containsExactly(RecruitingDecisionResult.PASSED);
        assertThat(captor.getValue().sortOrder()).isEqualTo(RecruitingDecisionHistorySortOrder.OLDEST);
        assertThat(captor.getValue().groupByDecider()).isTrue();
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 평가 이력은 빈 목록과 양수가 아닌 기수·지부·학교 ID를 거부한다")
    void decisionHistoriesRejectEmptyAndNonPositiveIdentifiers() {
        assertInvalidDecisionHistoryInput("gisuId: 0");
        assertInvalidDecisionHistoryInput("gisuId: 11, chapterIds: []");
        assertInvalidDecisionHistoryInput("gisuId: 11, schoolIds: []");
        assertInvalidDecisionHistoryInput("gisuId: 11, chapterIds: [-1]");
        assertInvalidDecisionHistoryInput("gisuId: 11, schoolIds: [0]");

        then(searchDecisionHistoryUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("GraphQL 모집 목록은 시즌별 그룹과 필터를 반환한다")
    void searchRoundGroups() {
        given(searchRoundGroupUseCase.searchRoundGroups(any())).willReturn(List.of(
            new RecruitingSeasonSummaryInfo(
                10L,
                11L,
                33L,
                "A 지부",
                22L,
                "A 학교",
                "운영진 메모",
                List.of(roundConfiguration())
            )
        ));

        graphQlTester.document("""
                query {
                  recruitingRoundGroups(input: {gisuId: 11, chapterId: 33, track: PLAN}) {
                    seasonId
                    chapterName
                    rounds { id availabilityFormId availabilityScheduleQuestionId }
                  }
                }
                """)
            .execute()
            .path("recruitingRoundGroups[0].chapterName")
            .entity(String.class)
            .isEqualTo("A 지부")
            .path("recruitingRoundGroups[0].rounds[0].id")
            .entity(String.class)
            .isEqualTo("20")
            .path("recruitingRoundGroups[0].rounds[0].availabilityFormId")
            .entity(String.class)
            .isEqualTo("100")
            .path("recruitingRoundGroups[0].rounds[0].availabilityScheduleQuestionId")
            .entity(String.class)
            .isEqualTo("200");

        ArgumentCaptor<RecruitingRoundGroupSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingRoundGroupSearchQuery.class);
        then(searchRoundGroupUseCase).should().searchRoundGroups(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().chapterId()).isEqualTo(33L);
        assertThat(captor.getValue().track()).isEqualTo(ChallengerTrack.PLAN);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 모집 목록은 양수가 아닌 기수 ID를 거부한다")
    void searchRoundGroupsRejectsNonPositiveGisuId() {
        graphQlTester.document("""
                query {
                  recruitingRoundGroups(input: {gisuId: 0}) { seasonId }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(searchRoundGroupUseCase).shouldHaveNoInteractions();
    }

    private void assertInvalidDecisionHistoryInput(String input) {
        graphQlTester.document("""
                query {
                  recruitingDecisionHistories(input: { %s }) { totalElements }
                }
                """.formatted(input))
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.get(0).getPath()).isEqualTo("recruitingDecisionHistories");
                assertThat(errors.get(0).getExtensions())
                    .containsEntry("code", CommonErrorCode.BAD_REQUEST.getCode());
            });
    }

    private RecruitingRoundConfigurationInfo roundConfiguration() {
        return new RecruitingRoundConfigurationInfo(
            20L,
            "15기 본모집",
            RecruitingRoundType.REGULAR,
            1,
            RecruitingRoundStatus.OPEN,
            List.of(ChallengerTrack.PLAN),
            false,
            java.time.Instant.parse("2026-08-01T00:00:00Z"),
            java.time.Instant.parse("2026-08-08T00:00:00Z"),
            java.time.Instant.parse("2026-08-10T00:00:00Z"),
            false,
            null,
            null,
            java.time.Instant.parse("2026-08-16T00:00:00Z"),
            100L,
            200L,
            "공고",
            "연락처"
        );
    }
}
