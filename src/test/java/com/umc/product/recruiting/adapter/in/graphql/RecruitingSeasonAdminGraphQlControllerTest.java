package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingSeasonCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ReplaceRecruitingSeasonTrackQuotasCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundConfigurationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingRoundSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingSeasonSummaryInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundStatus;
import com.umc.product.recruiting.domain.enums.RecruitingRoundType;
import com.umc.product.recruiting.domain.enums.RecruitingSeasonStatus;

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
    GetRecruitingSeasonConfigurationUseCase getSeasonConfigurationUseCase;
    @MockitoBean
    SearchRecruitingSeasonUseCase searchSeasonUseCase;
    @MockitoBean
    SearchRecruitingRoundUseCase searchRoundUseCase;
    @MockitoBean
    CreateRecruitingSeasonUseCase createSeasonUseCase;
    @MockitoBean
    UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    @MockitoBean
    ReplaceRecruitingSeasonTrackQuotasUseCase replaceQuotasUseCase;
    @MockitoBean
    CreateRecruitingRoundUseCase createRoundUseCase;
    @MockitoBean
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    @MockitoBean
    UpdateRecruitingRoundUseCase updateRoundUseCase;
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
        given(getApplicationQueryUseCase.getStatusSummary(11L, 22L, 40L))
            .willReturn(new RecruitingStatusSummaryInfo(
                3L,
                Map.of(RecruitingApplicationStatus.SUBMITTED, 3L)
            ));

        graphQlTester.document("""
                query {
                  recruitingStatusSummary(input: {gisuId: 11, schoolId: 22}) {
                    totalCount
                    countByStatus { status count }
                  }
                }
                """)
            .execute()
            .path("recruitingStatusSummary.totalCount")
            .entity(Long.class)
            .isEqualTo(3L);

        then(getApplicationQueryUseCase).should().getStatusSummary(11L, 22L, 40L);
    }

    @Test
    @DisplayName("GraphQL 시즌 목록은 현재 회원과 지부 필터를 query로 전달한다")
    void searchSeasons() {
        given(searchSeasonUseCase.searchSeasons(any())).willReturn(List.of(
            new RecruitingSeasonSummaryInfo(
                10L,
                11L,
                33L,
                "A 지부",
                22L,
                "A 학교",
                RecruitingSeasonStatus.ACTIVE,
                List.of(roundConfiguration())
            )
        ));

        graphQlTester.document("""
                query {
                  recruitingSeasons(input: {gisuId: 11, chapterId: 33}) {
                    seasonId
                    chapterName
                    rounds { id }
                  }
                }
                """)
            .execute()
            .path("recruitingSeasons[0].chapterName")
            .entity(String.class)
            .isEqualTo("A 지부")
            .path("recruitingSeasons[0].rounds[0].id")
            .entity(String.class)
            .isEqualTo("20");

        ArgumentCaptor<RecruitingSeasonSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingSeasonSearchQuery.class);
        then(searchSeasonUseCase).should().searchSeasons(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().chapterId()).isEqualTo(33L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 차수 목록은 현재 회원과 학교 및 시즌 필터를 query로 전달한다")
    void searchRounds() {
        given(searchRoundUseCase.searchRounds(any())).willReturn(List.of(
            new RecruitingRoundSummaryInfo(
                10L,
                11L,
                33L,
                "A 지부",
                22L,
                "A 학교",
                roundConfiguration()
            )
        ));

        graphQlTester.document("""
                query {
                  recruitingRounds(input: {gisuId: 11, schoolId: 22, seasonId: 10}) {
                    seasonId
                    schoolName
                    id
                  }
                }
                """)
            .execute()
            .path("recruitingRounds[0].schoolName")
            .entity(String.class)
            .isEqualTo("A 학교")
            .path("recruitingRounds[0].id")
            .entity(String.class)
            .isEqualTo("20");

        ArgumentCaptor<RecruitingRoundSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingRoundSearchQuery.class);
        then(searchRoundUseCase).should().searchRounds(captor.capture());
        assertThat(captor.getValue().gisuId()).isEqualTo(11L);
        assertThat(captor.getValue().schoolId()).isEqualTo(22L);
        assertThat(captor.getValue().seasonId()).isEqualTo(10L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(40L);
    }

    @Test
    @DisplayName("GraphQL 시즌 목록은 양수가 아닌 기수 ID를 거부한다")
    void searchSeasonsRejectsNonPositiveGisuId() {
        graphQlTester.document("""
                query {
                  recruitingSeasons(input: {gisuId: 0}) { seasonId }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(searchSeasonUseCase).shouldHaveNoInteractions();
    }

    private RecruitingRoundConfigurationInfo roundConfiguration() {
        return new RecruitingRoundConfigurationInfo(
            20L,
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
            null,
            "공고",
            "연락처"
        );
    }
}
