package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.form.application.port.in.query.dto.AnswerInfo;
import com.umc.product.form.domain.enums.QuestionType;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.OffsetPageRequest;
import com.umc.product.global.graphql.relay.RelayCursor;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationDetailInfo;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationSummaryInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationRegistrationStatus;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@GraphQlTest(RecruitingApplicationReviewGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingApplicationReviewGraphQlControllerTest {

    private static final Long REQUESTER_ID = 99L;
    private static final String ROUND_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.RECRUITING_ROUND, 20L);
    private static final String APPLICATION_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.RECRUITING_APPLICATION, 30L);

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    SearchRecruitingApplicationUseCase searchApplicationUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("평가용 지원서 Connection Query는 필터·커서·CurrentMember를 전달하고 edges를 반환한다")
    void searchApplications() {
        // Given
        given(searchApplicationUseCase.search(any())).willReturn(new PageImpl<>(
            List.of(summary(30L, true), summary(31L, false)),
            new OffsetPageRequest(5, 2),
            11
        ));

        // When & Then
        graphQlTester.document("""
                query {
                  recruitingRoundApplications(
                    roundId: "%s",
                    input: {statuses: [SUBMITTED, INTERVIEW_ASSIGNED], tracks: [PLAN, DESIGN]},
                    first: 2,
                    after: "%s"
                  ) {
                    edges {
                      cursor
                      node { applicationId documentEvaluatedByMe interviewEvaluatedByMe }
                    }
                    pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
                    totalCount
                  }
                }
                """.formatted(ROUND_GLOBAL_ID, RelayCursor.encodeOffset(4)))
            .execute()
            .path("recruitingRoundApplications.edges[0].cursor")
            .entity(String.class).isEqualTo(RelayCursor.encodeOffset(5))
            .path("recruitingRoundApplications.edges[0].node.applicationId")
            .entity(String.class).isEqualTo(APPLICATION_GLOBAL_ID)
            .path("recruitingRoundApplications.edges[0].node.documentEvaluatedByMe")
            .entity(Boolean.class).isEqualTo(true)
            .path("recruitingRoundApplications.edges[1].cursor")
            .entity(String.class).isEqualTo(RelayCursor.encodeOffset(6))
            .path("recruitingRoundApplications.pageInfo.hasNextPage")
            .entity(Boolean.class).isEqualTo(true)
            .path("recruitingRoundApplications.pageInfo.hasPreviousPage")
            .entity(Boolean.class).isEqualTo(true)
            .path("recruitingRoundApplications.pageInfo.startCursor")
            .entity(String.class).isEqualTo(RelayCursor.encodeOffset(5))
            .path("recruitingRoundApplications.pageInfo.endCursor")
            .entity(String.class).isEqualTo(RelayCursor.encodeOffset(6))
            .path("recruitingRoundApplications.totalCount")
            .entity(Long.class).isEqualTo(11L);

        ArgumentCaptor<RecruitingApplicationSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingApplicationSearchQuery.class);
        then(searchApplicationUseCase).should().search(captor.capture());
        assertThat(captor.getValue().roundId()).isEqualTo(20L);
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().statuses()).containsExactlyInAnyOrder(
            RecruitingApplicationStatus.SUBMITTED,
            RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        assertThat(captor.getValue().tracks()).containsExactlyInAnyOrder(
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        );
        assertThat(captor.getValue().pageable().getOffset()).isEqualTo(5L);
        assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("평가용 지원서 Query는 페이지 인자가 없으면 기본 first 20을 적용한다")
    void 평가용_지원서_Query는_페이지_인자가_없으면_기본_first_20을_적용한다() {
        // Given
        given(searchApplicationUseCase.search(any())).willReturn(new PageImpl<>(
            List.of(),
            new OffsetPageRequest(0, 20),
            0
        ));

        // When & Then
        graphQlTester.document("""
                query {
                  recruitingRoundApplications(roundId: "%s") {
                    edges { cursor }
                    pageInfo { hasNextPage hasPreviousPage startCursor endCursor }
                    totalCount
                  }
                }
                """.formatted(ROUND_GLOBAL_ID))
            .execute()
            .path("recruitingRoundApplications.edges").entityList(Object.class).hasSize(0)
            .path("recruitingRoundApplications.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(false)
            .path("recruitingRoundApplications.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(false)
            .path("recruitingRoundApplications.pageInfo.startCursor").valueIsNull()
            .path("recruitingRoundApplications.totalCount").entity(Long.class).isEqualTo(0L);

        ArgumentCaptor<RecruitingApplicationSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingApplicationSearchQuery.class);
        then(searchApplicationUseCase).should().search(captor.capture());
        assertThat(captor.getValue().statuses()).isEmpty();
        assertThat(captor.getValue().tracks()).isEmpty();
        assertThat(captor.getValue().pageable().getOffset()).isEqualTo(0L);
        assertThat(captor.getValue().pageable().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("평가용 지원서 Query는 first가 최대 페이지 크기를 넘으면 실행 전에 거부한다")
    void 평가용_지원서_Query는_first가_최대_페이지_크기를_넘으면_실행_전에_거부한다() {
        // When
        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  recruitingRoundApplications(roundId: "%s", first: 101) { totalCount }
                }
                """.formatted(ROUND_GLOBAL_ID))
            .execute();

        // Then
        response.errors().satisfy(errors -> {
            assertThat(errors).anySatisfy(error -> assertThat(error.getExtensions())
                .containsEntry("code", CommonErrorCode.BAD_REQUEST.getCode()));
        });
        then(searchApplicationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가용 지원서 Query는 다른 타입의 전역 ID를 실행 전에 거부한다")
    void 평가용_지원서_Query는_다른_타입의_전역_ID를_실행_전에_거부한다() {
        // When
        GraphQlTester.Response response = graphQlTester.document("""
                query {
                  recruitingRoundApplications(roundId: "%s") { totalCount }
                }
                """.formatted(GlobalId.encode(GlobalIdTypes.MEMBER, 20L)))
            .execute();

        // Then
        response.errors().satisfy(errors -> {
            assertThat(errors).anySatisfy(error -> assertThat(error.getExtensions())
                .containsEntry("code", CommonErrorCode.BAD_REQUEST.getCode()));
        });
        then(searchApplicationUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("평가용 지원서 상세 Query는 전역 ID를 디코딩해 UseCase에 전달하고 전역 ID로 응답한다")
    void 평가용_지원서_상세_Query는_전역_ID를_디코딩해_UseCase에_전달하고_전역_ID로_응답한다() {
        // Given
        given(searchApplicationUseCase.getDetail(20L, 30L, REQUESTER_ID)).willReturn(
            RecruitingApplicationDetailInfo.builder()
                .application(summary(30L, true))
                .formResponseId(500L)
                .answers(List.of(AnswerInfo.builder()
                    .questionId(700L)
                    .answeredAsType(QuestionType.LONG_TEXT)
                    .textValue("지원 동기")
                    .selectedOptions(List.of())
                    .fileIds(Set.of())
                    .times(Set.of())
                    .build()))
                .build()
        );

        // When & Then
        graphQlTester.document("""
                query {
                  recruitingRoundApplication(roundId: "%s", applicationId: "%s") {
                    application { applicationId status }
                    formResponseId
                    answers { questionId type textValue }
                  }
                }
                """.formatted(ROUND_GLOBAL_ID, APPLICATION_GLOBAL_ID))
            .execute()
            .path("recruitingRoundApplication.application.applicationId")
            .entity(String.class).isEqualTo(APPLICATION_GLOBAL_ID)
            .path("recruitingRoundApplication.formResponseId")
            .entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.FORM_RESPONSE, 500L))
            .path("recruitingRoundApplication.answers[0].questionId")
            .entity(String.class).isEqualTo(GlobalId.encode(GlobalIdTypes.FORM_QUESTION, 700L))
            .path("recruitingRoundApplication.answers[0].textValue")
            .entity(String.class).isEqualTo("지원 동기");

        then(searchApplicationUseCase).should().getDetail(20L, 30L, REQUESTER_ID);
    }

    private RecruitingApplicationSummaryInfo summary(Long applicationId, boolean documentEvaluatedByMe) {
        return RecruitingApplicationSummaryInfo.builder()
            .applicationId(applicationId)
            .applicantName("지원자")
            .email("applicant@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .status(RecruitingApplicationStatus.SUBMITTED)
            .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
            .submittedAt(Instant.parse("2026-08-07T00:00:00Z"))
            .documentEvaluatedByMe(documentEvaluatedByMe)
            .build();
    }
}
