package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.SubmitRecruitingApplicationEvaluationCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationEvaluationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationEvaluationDecision;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

@GraphQlTest(RecruitingEvaluationGraphQlController.class)
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingEvaluationGraphQlControllerTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingApplicationEvaluationUseCase getApplicationEvaluationUseCase;

    @MockitoBean
    SubmitRecruitingApplicationEvaluationUseCase submitApplicationEvaluationUseCase;

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
    @DisplayName("평가 확정 Mutation은 stage와 CurrentMember를 public UseCase에 전달한다")
    void 평가_확정_Mutation은_stage와_CurrentMember를_public_UseCase에_전달한다() {
        graphQlTester.document("""
                mutation {
                  submitRecruitingApplicationEvaluation(
                    applicationId: 20,
                    input: {stage: INTERVIEW, decision: REJECTED, comment: "불합격 의견"}
                  )
                }
                """)
            .execute()
            .path("submitRecruitingApplicationEvaluation")
            .entity(Boolean.class)
            .isEqualTo(true);

        ArgumentCaptor<SubmitRecruitingApplicationEvaluationCommand> captor =
            ArgumentCaptor.forClass(SubmitRecruitingApplicationEvaluationCommand.class);
        then(submitApplicationEvaluationUseCase).should().submit(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().stage()).isEqualTo(RecruitingEvaluatorStage.INTERVIEW);
        assertThat(captor.getValue().decision()).isEqualTo(RecruitingApplicationEvaluationDecision.REJECTED);
    }

    @Test
    @DisplayName("평가 Query는 제출 시각을 Instant scalar로 반환한다")
    void 평가_Query는_제출_시각을_Instant_scalar로_반환한다() {
        Instant submittedAt = Instant.parse("2026-08-11T00:00:00Z");
        given(getApplicationEvaluationUseCase.listVisibleEvaluations(
            20L,
            REQUESTER_ID,
            RecruitingEvaluatorStage.DOCUMENT
        )).willReturn(List.of(new RecruitingApplicationEvaluationInfo(
            70L,
            20L,
            REQUESTER_ID,
            RecruitingEvaluatorStage.DOCUMENT,
            RecruitingApplicationEvaluationDecision.APPROVED,
            "통과",
            submittedAt
        )));

        graphQlTester.document("""
                query {
                  recruitingApplicationEvaluations(applicationId: 20, stage: DOCUMENT) {
                    id
                    decision
                    submittedAt
                  }
                }
                """)
            .execute()
            .path("recruitingApplicationEvaluations[0].submittedAt")
            .entity(String.class)
            .isEqualTo(submittedAt.toString());
    }
}
