package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.CancelAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicApplicationInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingPublicRoundSearchQuery;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingPublicResultStatus;

@GraphQlTest(RecruitingGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
@DisplayName("RecruitingGraphQlSecurity")
class RecruitingGraphQlSecurityTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingFormQueryUseCase getFormQueryUseCase;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    CreateRecruitingApplicationDraftUseCase createDraftUseCase;

    @MockitoBean
    UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;

    @MockitoBean
    SubmitRecruitingApplicationUseCase submitApplicationUseCase;

    @MockitoBean
    CancelRecruitingApplicationUseCase cancelApplicationUseCase;

    @MockitoBean
    GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;

    @MockitoBean
    CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;

    @MockitoBean
    UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;

    @MockitoBean
    SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;

    @MockitoBean
    CancelAnonymousRecruitingApplicationUseCase cancelAnonymousApplicationUseCase;

    @MockitoBean
    SearchPublicRecruitingRoundUseCase searchPublicRoundUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void clearSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비로그인 GraphQL 지원서 생성은 FORBIDDEN으로 거부한다")
    void 비로그인_GraphQL_지원서_생성은_FORBIDDEN으로_거부한다() {
        graphQlTester.document("""
                mutation {
                  createRecruitingApplicationDraft(input: {
                    applicationFormId: 100,
                    applicantName: "지원자",
                    applicantEmail: "applicant@example.invalid",
                    firstChoice: PLAN
                  }) { applicationId }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst().getExtensions())
                    .containsEntry("code", CommonErrorCode.FORBIDDEN.getCode());
            });

        then(createDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("로그인 지원서 Query는 CurrentMember의 ID만 public UseCase에 전달한다")
    void 로그인_지원서_Query는_CurrentMember_ID만_public_UseCase에_전달한다() {
        authenticate();
        given(getApplicationQueryUseCase.getById(20L, REQUESTER_ID)).willReturn(applicationInfo());

        graphQlTester.document("""
                query {
                  recruitingApplication(applicationId: 20) {
                    applicationId
                    status
                    registrationStatus
                    firstChoice
                    secondChoice
                    acceptedTrack
                  }
                }
                """)
            .execute()
            .path("recruitingApplication")
            .matchesJson("""
                {
                  "applicationId": "20",
                  "status": "FINAL_PASSED",
                  "registrationStatus": "READY",
                  "firstChoice": "PLAN",
                  "secondChoice": "DESIGN",
                  "acceptedTrack": "DESIGN"
                }
                """);

        then(getApplicationQueryUseCase).should().getById(20L, REQUESTER_ID);
    }

    @Test
    @DisplayName("익명 지원서 초안 생성 Mutation은 로그인 없이 실행된다")
    void 익명_지원서_초안_생성_Mutation은_로그인_없이_실행된다() {
        given(createAnonymousDraftUseCase.createAnonymousDraft(org.mockito.ArgumentMatchers.any()))
            .willReturn(RecruitingApplicationCreatedInfo.of(30L, "A1B2C3", RecruitingApplicationStatus.DRAFT));

        graphQlTester.document("""
                mutation {
                  createAnonymousRecruitingApplicationDraft(input: {
                    applicationFormId: 100,
                    applicantName: "지원자",
                    applicantEmail: "applicant@example.invalid",
                    firstChoice: PLAN,
                    privacyTermId: 3,
                    privacyAgreed: true
                  }) {
                    applicationId
                    applicationKey
                    status
                  }
                }
                """)
            .execute()
            .path("createAnonymousRecruitingApplicationDraft")
            .matchesJson("""
                {
                  "applicationId": "30",
                  "applicationKey": "A1B2C3",
                  "status": "DRAFT"
                }
                """);
    }

    @Test
    @DisplayName("credential Query는 공개 결과만 반환하고 application key를 응답 계약에 두지 않는다")
    void credential_Query는_공개_결과만_반환한다() {
        given(getAnonymousApplicationUseCase.getByCredential("applicant@example.invalid", "A1B2C3"))
            .willReturn(RecruitingPublicApplicationInfo.builder()
                .applicationId(30L)
                .gisuId(11L)
                .roundId(20L)
                .applicantName("지원자")
                .applicantEmail("applicant@example.invalid")
                .firstChoice(ChallengerTrack.PLAN)
                .submitted(true)
                .editable(true)
                .documentResult(RecruitingPublicResultStatus.PENDING)
                .finalResult(RecruitingPublicResultStatus.PENDING)
                .answers(List.of())
                .build());

        graphQlTester.document("""
                query {
                  recruitingApplicationByCredential(input: {
                    email: "applicant@example.invalid",
                    applicationKey: "A1B2C3"
                  }) {
                    applicationId
                    applicantEmail
                    documentResult
                    finalResult
                  }
                }
                """)
            .execute()
            .path("recruitingApplicationByCredential")
            .matchesJson("""
                {
                  "applicationId": "30",
                  "applicantEmail": "applicant@example.invalid",
                  "documentResult": "PENDING",
                  "finalResult": "PENDING"
                }
                """);
    }

    @Test
    @DisplayName("공개 모집 Query는 복수 학교·Round와 학교명 필터를 전달한다")
    void publicRoundQueryBindsMultipleFilters() {
        given(searchPublicRoundUseCase.searchPublicRounds(org.mockito.ArgumentMatchers.any()))
            .willReturn(List.of());

        graphQlTester.document("""
                query {
                  publicRecruitingRounds(input: {
                    gisuId: 11,
                    schoolIds: [22, 23],
                    roundIds: [31, 32],
                    schoolName: "대학교"
                  }) { seasonId }
                }
                """)
            .execute()
            .path("publicRecruitingRounds")
            .entityList(Object.class)
            .hasSize(0);

        ArgumentCaptor<RecruitingPublicRoundSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingPublicRoundSearchQuery.class);
        then(searchPublicRoundUseCase).should().searchPublicRounds(captor.capture());
        assertThat(captor.getValue().schoolIds()).containsExactlyInAnyOrder(22L, 23L);
        assertThat(captor.getValue().roundIds()).containsExactlyInAnyOrder(31L, 32L);
        assertThat(captor.getValue().schoolName()).isEqualTo("대학교");
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    private static RecruitingApplicationInfo applicationInfo() {
        return new RecruitingApplicationInfo(
            20L,
            RecruitingApplicationStatus.FINAL_PASSED,
            RecruitingApplicationRegistrationStatus.READY,
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN,
            ChallengerTrack.DESIGN
        );
    }
}
