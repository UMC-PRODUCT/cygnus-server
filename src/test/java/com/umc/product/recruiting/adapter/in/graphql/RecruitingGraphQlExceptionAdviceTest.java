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
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;

@GraphQlTest(RecruitingInterviewGraphQlController.class)
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class, RecruitingGraphQlPermissionSupport.class})
@DisplayName("RecruitingGraphQlExceptionAdvice")
class RecruitingGraphQlExceptionAdviceTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingInterviewEvaluationUseCase getEvaluationUseCase;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    GetRecruitingFormQueryUseCase getFormQueryUseCase;

    @MockitoBean
    AssignRecruitingInterviewUseCase assignInterviewUseCase;

    @MockitoBean
    SkipRecruitingInterviewUseCase skipInterviewUseCase;

    @MockitoBean
    FindRecruitingInterviewScheduleCandidatesUseCase findScheduleCandidatesUseCase;

    @MockitoBean
    SendRecruitingInterviewGuideUseCase sendInterviewGuideUseCase;

    @MockitoBean
    SaveRecruitingInterviewEvaluationUseCase saveEvaluationUseCase;

    @MockitoBean
    SubmitRecruitingInterviewEvaluationUseCase submitEvaluationUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("면접 배정 일시 형식이 올바르지 않으면 BAD_REQUEST GraphQL error를 반환한다")
    void 면접_배정_일시_형식이_올바르지_않으면_BAD_REQUEST_GraphQL_error를_반환한다() {
        given(getApplicationQueryUseCase.isApplicationBelongsToSeason(20L, 10L)).willReturn(true);

        graphQlTester.document("""
                mutation {
                  assignRecruitingInterview(
                    seasonId: 10,
                    applicationId: 20,
                    input: {
                      interviewerMemberId: 30,
                      startsAt: "not-instant",
                      endsAt: "2026-07-02T10:00:00Z",
                      location: "Room A"
                    }
                  ) {
                    id
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "assignRecruitingInterview", CommonErrorCode.BAD_REQUEST));

        then(assignInterviewUseCase).shouldHaveNoInteractions();
    }

    private static void assertCommonError(List<ResponseError> errors, String path, CommonErrorCode code) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getPath()).isEqualTo(path);
        assertThat(errors.get(0).getExtensions()).containsEntry("code", code.getCode());
    }
}
