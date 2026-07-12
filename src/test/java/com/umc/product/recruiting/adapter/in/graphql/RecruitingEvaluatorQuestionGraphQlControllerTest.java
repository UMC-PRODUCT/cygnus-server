package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingRoundEvaluatorCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingRoundEvaluatorUseCase;
import com.umc.product.recruiting.domain.enums.RecruitingEvaluatorStage;

@GraphQlTest({RecruitingEvaluatorAdminGraphQlController.class, RecruitingQuestionGraphQlController.class})
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    RecruitingGraphQlPermissionSupport.class
})
class RecruitingEvaluatorQuestionGraphQlControllerTest {

    private static final Long REQUESTER_ID = 40L;

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @MockitoBean
    GetRecruitingRoundEvaluatorUseCase getRoundEvaluatorUseCase;

    @MockitoBean
    GetRecruitingInterviewQuestionUseCase getInterviewQuestionUseCase;

    @MockitoBean
    ManageRecruitingRoundEvaluatorUseCase manageRoundEvaluatorUseCase;

    @MockitoBean
    ManageRecruitingRoundInterviewQuestionUseCase manageRoundQuestionUseCase;

    @MockitoBean
    ManageRecruitingApplicationInterviewQuestionUseCase manageApplicationQuestionUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    CurrentMemberProvider currentMemberProvider;

    @BeforeEach
    void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
        given(getApplicationQueryUseCase.isRoundBelongsToSeason(20L, 10L)).willReturn(true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("평가자 추가 Mutation은 대상 evaluator와 CurrentMember를 public UseCase에 전달한다")
    void 평가자_추가_Mutation은_대상_evaluator와_CurrentMember를_public_UseCase에_전달한다() {
        given(manageRoundEvaluatorUseCase.addEvaluator(any())).willReturn(70L);

        graphQlTester.document("""
                mutation {
                  addRecruitingRoundEvaluator(
                    seasonId: 10,
                    roundId: 20,
                    input: {evaluatorMemberId: 50, stage: DOCUMENT}
                  ) { id }
                }
                """)
            .execute()
            .path("addRecruitingRoundEvaluator.id")
            .entity(String.class)
            .isEqualTo("70");

        ArgumentCaptor<RecruitingRoundEvaluatorCommand> captor =
            ArgumentCaptor.forClass(RecruitingRoundEvaluatorCommand.class);
        then(manageRoundEvaluatorUseCase).should().addEvaluator(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
        assertThat(captor.getValue().memberId()).isEqualTo(50L);
        assertThat(captor.getValue().stage()).isEqualTo(RecruitingEvaluatorStage.DOCUMENT);
    }

    @Test
    @DisplayName("공통 질문 생성 Mutation은 CurrentMember를 public UseCase에 전달한다")
    void 공통_질문_생성_Mutation은_CurrentMember를_public_UseCase에_전달한다() {
        given(manageRoundQuestionUseCase.createRoundQuestion(any())).willReturn(80L);

        graphQlTester.document("""
                mutation {
                  createRecruitingRoundInterviewQuestion(
                    roundId: 20,
                    input: {content: "협업 경험", orderNo: 1}
                  ) { id }
                }
                """)
            .execute()
            .path("createRecruitingRoundInterviewQuestion.id")
            .entity(String.class)
            .isEqualTo("80");

        ArgumentCaptor<CreateRecruitingRoundInterviewQuestionCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingRoundInterviewQuestionCommand.class);
        then(manageRoundQuestionUseCase).should().createRoundQuestion(captor.capture());
        assertThat(captor.getValue().requesterMemberId()).isEqualTo(REQUESTER_ID);
    }
}
