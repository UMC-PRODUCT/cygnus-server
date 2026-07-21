package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.List;

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
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingApplicationUseCase;
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

    @Test
    @DisplayName("평가용 지원서 Query는 필터·페이지와 CurrentMember를 전달한다")
    void searchApplications() {
        given(searchApplicationUseCase.search(any())).willReturn(new PageImpl<>(
            List.of(summary()),
            PageRequest.of(1, 10),
            11
        ));

        graphQlTester.document("""
                query {
                  recruitingRoundApplications(
                    roundId: 20,
                    input: {statuses: [SUBMITTED, INTERVIEW_ASSIGNED], tracks: [PLAN, DESIGN], page: 1, size: 10}
                  ) {
                    content { applicationId documentEvaluatedByMe }
                    totalElements
                  }
                }
                """)
            .execute()
            .path("recruitingRoundApplications.content[0].applicationId")
            .entity(String.class)
            .isEqualTo("30")
            .path("recruitingRoundApplications.totalElements")
            .entity(Long.class)
            .isEqualTo(11L);

        ArgumentCaptor<RecruitingApplicationSearchQuery> captor =
            ArgumentCaptor.forClass(RecruitingApplicationSearchQuery.class);
        then(searchApplicationUseCase).should().search(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().statuses()).containsExactlyInAnyOrder(
            RecruitingApplicationStatus.SUBMITTED,
            RecruitingApplicationStatus.INTERVIEW_ASSIGNED
        );
        assertThat(captor.getValue().tracks()).containsExactlyInAnyOrder(
            ChallengerTrack.PLAN,
            ChallengerTrack.DESIGN
        );
        assertThat(captor.getValue().pageable().getPageNumber()).isEqualTo(1);
    }

    private RecruitingApplicationSummaryInfo summary() {
        return RecruitingApplicationSummaryInfo.builder()
            .applicationId(30L)
            .applicantName("지원자")
            .email("applicant@example.com")
            .firstChoice(ChallengerTrack.PLAN)
            .status(RecruitingApplicationStatus.SUBMITTED)
            .registrationStatus(RecruitingApplicationRegistrationStatus.NOT_READY)
            .submittedAt(Instant.parse("2026-08-07T00:00:00Z"))
            .documentEvaluatedByMe(true)
            .build();
    }
}
