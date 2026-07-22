package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingPublicRoundSearchGraphQlRequest;
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
import com.umc.product.recruiting.application.port.in.query.GetRecruitingResourceUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchPublicRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationCreatedInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationResourceInfo;
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
    GetRecruitingResourceUseCase getResourceUseCase;

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
    @DisplayName("비로그인 GraphQL 지원서 생성은 개인정보 동의가 없으면 거부한다")
    void 비로그인_GraphQL_지원서_생성은_개인정보_동의가_없으면_거부한다() {
        graphQlTester.document("""
                mutation {
                  createRecruitingApplication(input: {
                    applicationFormId: 100,
                    applicantName: "지원자",
                    applicantEmail: "applicant@example.invalid",
                    firstChoice: PLAN
                  }) { application { id } }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.getFirst().getExtensions())
                    .containsEntry("code", CommonErrorCode.BAD_REQUEST.getCode());
            });

        then(createAnonymousDraftUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("로그인 지원서 Query는 CurrentMember의 ID만 public UseCase에 전달한다")
    void 로그인_지원서_Query는_CurrentMember_ID만_public_UseCase에_전달한다() {
        authenticate();
        given(getResourceUseCase.getApplication(20L, REQUESTER_ID)).willReturn(applicationInfo());

        graphQlTester.document("""
                query {
                  recruitingApplication(access: {applicationId: 20}) {
                    id
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
                  "id": "20",
                  "status": "FINAL_PASSED",
                  "registrationStatus": "READY",
                  "firstChoice": "PLAN",
                  "secondChoice": "DESIGN",
                  "acceptedTrack": "DESIGN"
                }
                """);

        then(getResourceUseCase).should().getApplication(20L, REQUESTER_ID);
    }

    @Test
    @DisplayName("익명 지원서 초안 생성 Mutation은 로그인 없이 실행된다")
    void 익명_지원서_초안_생성_Mutation은_로그인_없이_실행된다() {
        given(createAnonymousDraftUseCase.createAnonymousDraft(org.mockito.ArgumentMatchers.any()))
            .willReturn(RecruitingApplicationCreatedInfo.of(30L, "A1B2C3", RecruitingApplicationStatus.DRAFT));
        given(getAnonymousApplicationUseCase.getByCredential("applicant@example.invalid", "A1B2C3"))
            .willReturn(publicApplicationInfo());

        graphQlTester.document("""
                mutation {
                  createRecruitingApplication(input: {
                    applicationFormId: 100,
                    applicantName: "지원자",
                    applicantEmail: "applicant@example.invalid",
                    firstChoice: PLAN,
                    privacyTermId: 3,
                    privacyAgreed: true
                  }) {
                    application { id status }
                    credential { email applicationKey }
                  }
                }
                """)
            .execute()
            .path("createRecruitingApplication")
            .matchesJson("""
                {
                  "application": {"id": "30", "status": "DRAFT"},
                  "credential": {
                    "email": "applicant@example.invalid",
                    "applicationKey": "A1B2C3"
                  }
                }
                """);
    }

    @Test
    @DisplayName("credential Query는 공개 결과만 반환하고 application key를 응답 계약에 두지 않는다")
    void credential_Query는_공개_결과만_반환한다() {
        given(getAnonymousApplicationUseCase.getByCredential("applicant@example.invalid", "A1B2C3"))
            .willReturn(publicApplicationInfo());

        graphQlTester.document("""
                query {
                  recruitingApplication(access: {credential: {
                    email: "applicant@example.invalid", applicationKey: "A1B2C3"
                  }}) {
                    id
                    private { applicantEmail documentResult finalResult }
                  }
                }
                """)
            .execute()
            .path("recruitingApplication")
            .matchesJson("""
                {
                  "id": "30",
                  "private": {
                    "applicantEmail": "applicant@example.invalid",
                    "documentResult": "PENDING",
                    "finalResult": "PENDING"
                  }
                }
                """);
    }

    @Test
    @DisplayName("공개 모집 Query는 복수 학교·Round와 학교명 필터를 전달한다")
    void publicRoundQueryBindsMultipleFilters() {
        RecruitingPublicRoundSearchQuery query = new RecruitingPublicRoundSearchGraphQlRequest(
            11L,
            null,
            List.of(22L, 23L),
            List.of(31L, 32L),
            "대학교",
            null,
            null,
            null,
            null
        ).toQuery();

        assertThat(query.schoolIds()).containsExactlyInAnyOrder(22L, 23L);
        assertThat(query.roundIds()).containsExactlyInAnyOrder(31L, 32L);
        assertThat(query.schoolName()).isEqualTo("대학교");
    }

    private static void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    private static RecruitingApplicationResourceInfo applicationInfo() {
        return new RecruitingApplicationResourceInfo(
            20L,
            30L,
            10L,
            RecruitingApplicationStatus.FINAL_PASSED,
            RecruitingApplicationRegistrationStatus.READY,
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN,
            ChallengerTrack.DESIGN,
            true,
            false
        );
    }

    private static RecruitingPublicApplicationInfo publicApplicationInfo() {
        return RecruitingPublicApplicationInfo.builder()
            .applicationId(30L)
            .roundId(40L)
            .seasonId(10L)
            .status(RecruitingApplicationStatus.DRAFT)
            .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
            .applicantName("지원자")
            .applicantEmail("applicant@example.invalid")
            .firstChoice(ChallengerTrack.PLAN)
            .submitted(false)
            .editable(true)
            .documentResult(RecruitingPublicResultStatus.PENDING)
            .finalResult(RecruitingPublicResultStatus.PENDING)
            .answers(List.of())
            .build();
    }
}
