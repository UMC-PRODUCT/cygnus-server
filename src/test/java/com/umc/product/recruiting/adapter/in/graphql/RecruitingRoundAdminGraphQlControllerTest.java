package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

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
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CloneRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DeleteRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.ReplaceRecruitingSeasonTrackQuotasUseCase;
import com.umc.product.recruiting.application.port.in.command.RestoreRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.command.dto.UpdateRecruitingRoundCommand;
import com.umc.product.recruiting.application.port.in.query.CheckRecruitingRoundTitleUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingEvaluationStatisticsUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingSeasonConfigurationUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundGroupUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingAdminFormStructureInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationFormStatus;
import com.umc.product.recruiting.domain.enums.RecruitingFormSectionType;

@GraphQlTest(RecruitingAdminGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingRoundAdminGraphQlControllerTest {

    @Autowired
    GraphQlTester graphQlTester;
    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    @MockitoBean
    GetRecruitingEvaluationStatisticsUseCase getEvaluationStatisticsUseCase;
    @MockitoBean
    SearchRecruitingDecisionHistoryUseCase searchDecisionHistoryUseCase;
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
    RestoreRecruitingRoundUseCase restoreRoundUseCase;
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
    @DisplayName("GraphQL 지원 Form 구조 조회는 section 정책을 함께 반환한다")
    void recruitingAdminFormStructure() {
        given(getRecruitingFormQueryUseCase.getAdminFormStructure(10L, 20L))
            .willReturn(RecruitingAdminFormStructureInfo.builder()
                .exists(true)
                .applicationFormId(30L)
                .formId(100L)
                .title("15기 본모집")
                .status(RecruitingApplicationFormStatus.DRAFT)
                .sections(List.of(RecruitingAdminFormStructureInfo.SectionInfo.builder()
                    .sectionId(300L)
                    .clientKey("section-300")
                    .title("PLAN 파트")
                    .orderNo(1L)
                    .type(RecruitingFormSectionType.TRACK)
                    .track(ChallengerTrack.PLAN)
                    .questions(List.of(RecruitingAdminFormStructureInfo.QuestionInfo.builder()
                        .questionId(400L)
                        .title("지원 동기")
                        .type(QuestionType.LONG_TEXT)
                        .required(true)
                        .orderNo(1L)
                        .options(List.of(RecruitingAdminFormStructureInfo.OptionInfo.builder()
                            .optionId(500L)
                            .content("다음")
                            .orderNo(1L)
                            .nextSectionId(300L)
                            .nextSectionKey("section-300")
                            .build()))
                        .build()))
                    .build()))
                .build());

        graphQlTester.document("""
                query {
                  recruitingAdminFormStructure(seasonId: 10, roundId: 20) {
                    exists
                    applicationFormId
                    status
                    sections {
                      sectionId
                      clientKey
                      type
                      track
                      questions { options { nextSectionId nextSectionKey } }
                    }
                  }
                }
                """)
            .execute()
            .path("recruitingAdminFormStructure.exists").entity(Boolean.class).isEqualTo(true)
            .path("recruitingAdminFormStructure.applicationFormId").entity(String.class).isEqualTo("30")
            .path("recruitingAdminFormStructure.sections[0].clientKey").entity(String.class).isEqualTo("section-300")
            .path("recruitingAdminFormStructure.sections[0].type").entity(String.class).isEqualTo("TRACK")
            .path("recruitingAdminFormStructure.sections[0].track").entity(String.class).isEqualTo("PLAN")
            .path("recruitingAdminFormStructure.sections[0].questions[0].options[0].nextSectionId")
            .entity(String.class).isEqualTo("300")
            .path("recruitingAdminFormStructure.sections[0].questions[0].options[0].nextSectionKey")
            .entity(String.class).isEqualTo("section-300");
    }

    @Test
    @DisplayName("GraphQL 지원 Form 구조 조회는 Form 미생성 차수에 빈 구조를 반환한다")
    void recruitingAdminFormStructureEmpty() {
        given(getRecruitingFormQueryUseCase.getAdminFormStructure(10L, 20L))
            .willReturn(RecruitingAdminFormStructureInfo.empty());

        graphQlTester.document("""
                query {
                  recruitingAdminFormStructure(seasonId: 10, roundId: 20) {
                    exists
                    applicationFormId
                    sections { sectionId }
                  }
                }
                """)
            .execute()
            .path("recruitingAdminFormStructure.exists").entity(Boolean.class).isEqualTo(false)
            .path("recruitingAdminFormStructure.applicationFormId").valueIsNull()
            .path("recruitingAdminFormStructure.sections").entityList(Object.class).hasSize(0);
    }

    @Test
    @DisplayName("GraphQL 면접 차수 생성은 전체 설정을 Instant command로 전달한다")
    void createInterviewRound() {
        given(createRoundUseCase.createRound(any())).willReturn(20L);

        graphQlTester.document(createRoundMutation("2026-08-01T00:00:00Z"))
            .execute()
            .path("createRecruitingRound.id")
            .entity(String.class)
            .isEqualTo("20");

        ArgumentCaptor<CreateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingRoundCommand.class);
        then(createRoundUseCase).should().createRound(captor.capture());
        assertThat(captor.getValue().configuration().documentStartAt())
            .isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        assertThat(captor.getValue().configuration().availabilityFormId()).isEqualTo(100L);
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("GraphQL 면접 없는 차수 변경은 null 면접 설정을 전달한다")
    void updateNoInterviewRound() {
        given(getApplicationQueryUseCase.isRoundBelongsToSeason(20L, 10L)).willReturn(true);

        graphQlTester.document("""
                mutation {
                  updateRecruitingRound(
                    seasonId: 10,
                    roundId: 20,
                    input: {
                      title: "15기 본모집",
                      recruitableTracks: [PLAN],
                      secondChoiceEnabled: false,
                      documentStartAt: "2026-08-01T00:00:00Z",
                      documentEndAt: "2026-08-08T00:00:00Z",
                      documentResultPublishedAt: "2026-08-10T00:00:00Z",
                      interviewRequired: false,
                      finalResultPublishedAt: "2026-08-16T00:00:00Z"
                    }
                  )
                }
                """)
            .execute()
            .path("updateRecruitingRound")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<UpdateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingRoundCommand.class);
        then(updateRoundUseCase).should().updateRound(captor.capture());
        assertThat(captor.getValue().configuration().interviewStartAt()).isNull();
        assertThat(captor.getValue().configuration().availabilityFormId()).isNull();
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isNull();
    }

    @Test
    @DisplayName("GraphQL 면접 차수 변경은 availability Form과 SCHEDULE 질문 ID를 함께 전달한다")
    void updateInterviewRound() {
        given(getApplicationQueryUseCase.isRoundBelongsToSeason(20L, 10L)).willReturn(true);

        graphQlTester.document("""
                mutation {
                  updateRecruitingRound(
                    seasonId: 10,
                    roundId: 20,
                    input: {
                      title: "15기 본모집",
                      recruitableTracks: [PLAN],
                      secondChoiceEnabled: false,
                      documentStartAt: "2026-08-01T00:00:00Z",
                      documentEndAt: "2026-08-08T00:00:00Z",
                      documentResultPublishedAt: "2026-08-10T00:00:00Z",
                      interviewRequired: true,
                      interviewStartAt: "2026-08-11T00:00:00Z",
                      interviewEndAt: "2026-08-14T00:00:00Z",
                      finalResultPublishedAt: "2026-08-16T00:00:00Z",
                      availabilityFormId: 100,
                      availabilityScheduleQuestionId: 200
                    }
                  )
                }
                """)
            .execute()
            .path("updateRecruitingRound")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<UpdateRecruitingRoundCommand> captor =
            ArgumentCaptor.forClass(UpdateRecruitingRoundCommand.class);
        then(updateRoundUseCase).should().updateRound(captor.capture());
        assertThat(captor.getValue().configuration().interviewRequired()).isTrue();
        assertThat(captor.getValue().configuration().availabilityFormId()).isEqualTo(100L);
        assertThat(captor.getValue().configuration().availabilityScheduleQuestionId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("GraphQL 차수 생성의 잘못된 Instant는 BAD_REQUEST error를 반환한다")
    void createRoundRejectsMalformedInstant() {
        graphQlTester.document(createRoundMutation("not-an-instant"))
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));
        then(createRoundUseCase).shouldHaveNoInteractions();
    }

    private String createRoundMutation(String documentStartAt) {
        return """
            mutation {
              createRecruitingRound(
                seasonId: 10,
                input: {
                  title: "15기 본모집",
                  type: REGULAR,
                  recruitableTracks: [PLAN, DESIGN],
                  secondChoiceEnabled: true,
                  documentStartAt: "%s",
                  documentEndAt: "2026-08-08T00:00:00Z",
                  documentResultPublishedAt: "2026-08-10T00:00:00Z",
                  interviewRequired: true,
                  interviewStartAt: "2026-08-11T00:00:00Z",
                  interviewEndAt: "2026-08-14T00:00:00Z",
                  finalResultPublishedAt: "2026-08-16T00:00:00Z",
                  availabilityFormId: 100,
                  availabilityScheduleQuestionId: 200,
                  announcement: "안내",
                  contactText: "문의"
                }
              ) { id }
            }
            """.formatted(documentStartAt);
    }
}
