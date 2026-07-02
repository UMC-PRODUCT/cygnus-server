package com.umc.product.recruiting.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.product.global.security.CurrentMemberProvider;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.recruiting.adapter.in.graphql.dto.CreateRecruitingApplicationDraftGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.RecruitingDecisionGraphQlRequest;
import com.umc.product.recruiting.adapter.in.graphql.dto.SaveRecruitingInterviewEvaluationGraphQlRequest;
import com.umc.product.recruiting.application.port.in.command.AssignRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.FindRecruitingInterviewScheduleCandidatesUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.SaveRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.SendRecruitingInterviewGuideUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CreateRecruitingApplicationDraftCommand;
import com.umc.product.recruiting.application.port.in.command.dto.RecruitingDecisionStatus;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewEvaluationUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingApplicationInfo;
import com.umc.product.recruiting.domain.enums.RecruitingApplicationStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecruitingGraphQlSecurity")
class RecruitingGraphQlSecurityTest {

    private static final Long SEASON_ID = 10L;
    private static final Long APPLICATION_ID = 20L;
    private static final Long ASSIGNMENT_ID = 30L;
    private static final Long REQUESTER_ID = 40L;

    @Mock
    GetRecruitingFormQueryUseCase getFormQueryUseCase;

    @Mock
    GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;

    @Mock
    GetRecruitingInterviewEvaluationUseCase getEvaluationUseCase;

    @Mock
    CreateRecruitingApplicationDraftUseCase createDraftUseCase;

    @Mock
    UpdateRecruitingApplicationDraftUseCase updateDraftUseCase;

    @Mock
    SubmitRecruitingApplicationUseCase submitApplicationUseCase;

    @Mock
    CancelRecruitingApplicationUseCase cancelApplicationUseCase;

    @Mock
    CreateRecruitingSeasonUseCase createSeasonUseCase;

    @Mock
    UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;

    @Mock
    CreateRecruitingRoundUseCase createRoundUseCase;

    @Mock
    UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;

    @Mock
    LinkRecruitingApplicationFormUseCase linkFormUseCase;

    @Mock
    PublishRecruitingApplicationFormUseCase publishFormUseCase;

    @Mock
    CloseRecruitingApplicationFormUseCase closeFormUseCase;

    @Mock
    DecideRecruitingDocumentUseCase decideDocumentUseCase;

    @Mock
    DecideRecruitingFinalUseCase decideFinalUseCase;

    @Mock
    ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;

    @Mock
    AssignRecruitingInterviewUseCase assignInterviewUseCase;

    @Mock
    SkipRecruitingInterviewUseCase skipInterviewUseCase;

    @Mock
    FindRecruitingInterviewScheduleCandidatesUseCase findScheduleCandidatesUseCase;

    @Mock
    SendRecruitingInterviewGuideUseCase sendInterviewGuideUseCase;

    @Mock
    SaveRecruitingInterviewEvaluationUseCase saveEvaluationUseCase;

    @Mock
    SubmitRecruitingInterviewEvaluationUseCase submitEvaluationUseCase;

    @Mock
    com.umc.product.authorization.application.port.in.CheckPermissionUseCase checkPermissionUseCase;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("비로그인 GraphQL 지원서 생성은 입력 applicantMemberId를 신뢰하지 않는다")
    void 비로그인_GraphQL_지원서_생성은_입력_applicantMemberId를_신뢰하지_않는다() {
        RecruitingGraphQlController controller = publicController();
        given(createDraftUseCase.createDraft(any(CreateRecruitingApplicationDraftCommand.class)))
            .willReturn(RecruitingApplicationInfo.from(1L, "APP-1", RecruitingApplicationStatus.DRAFT));

        controller.createRecruitingApplicationDraft(
            null,
            new CreateRecruitingApplicationDraftGraphQlRequest(100L, 999L, "anonymous-key", "a***@umc.test")
        );

        ArgumentCaptor<CreateRecruitingApplicationDraftCommand> captor =
            ArgumentCaptor.forClass(CreateRecruitingApplicationDraftCommand.class);
        then(createDraftUseCase).should().createDraft(captor.capture());
        assertThat(captor.getValue().applicantMemberId()).isNull();
    }

    @Test
    @DisplayName("최종 합불 결정은 application이 season에 속하지 않으면 권한 검사 전에 거부한다")
    void 최종_합불_결정은_application이_season에_속하지_않으면_권한_검사_전에_거부한다() {
        authenticateRequester();
        RecruitingAdminGraphQlController controller = adminController();
        given(getApplicationQueryUseCase.isApplicationBelongsToSeason(APPLICATION_ID, SEASON_ID))
            .willReturn(false);

        assertThatThrownBy(() -> controller.decideRecruitingFinal(
            null,
            SEASON_ID,
            APPLICATION_ID,
            new RecruitingDecisionGraphQlRequest(RecruitingDecisionStatus.PASS, "pass")
        )).isInstanceOf(AccessDeniedException.class);

        then(getApplicationQueryUseCase).should().isApplicationBelongsToSeason(APPLICATION_ID, SEASON_ID);
        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(decideFinalUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("면접 평가 제출은 assignment가 season에 속하지 않으면 권한 검사 전에 거부한다")
    void 면접_평가_제출은_assignment가_season에_속하지_않으면_권한_검사_전에_거부한다() {
        authenticateRequester();
        RecruitingInterviewGraphQlController controller = interviewController();
        given(getEvaluationUseCase.isAssignmentBelongsToSeason(ASSIGNMENT_ID, SEASON_ID))
            .willReturn(false);

        assertThatThrownBy(() -> controller.submitRecruitingInterviewEvaluation(
            null,
            SEASON_ID,
            ASSIGNMENT_ID,
            new SaveRecruitingInterviewEvaluationGraphQlRequest(5, "good")
        )).isInstanceOf(AccessDeniedException.class);

        then(getEvaluationUseCase).should().isAssignmentBelongsToSeason(ASSIGNMENT_ID, SEASON_ID);
        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(submitEvaluationUseCase).shouldHaveNoInteractions();
    }

    private RecruitingGraphQlController publicController() {
        return new RecruitingGraphQlController(
            getFormQueryUseCase,
            getApplicationQueryUseCase,
            createDraftUseCase,
            updateDraftUseCase,
            submitApplicationUseCase,
            cancelApplicationUseCase,
            permissionSupport()
        );
    }

    private RecruitingAdminGraphQlController adminController() {
        return new RecruitingAdminGraphQlController(
            getApplicationQueryUseCase,
            getFormQueryUseCase,
            createSeasonUseCase,
            updateSeasonStatusUseCase,
            createRoundUseCase,
            updateRoundStatusUseCase,
            linkFormUseCase,
            publishFormUseCase,
            closeFormUseCase,
            decideDocumentUseCase,
            decideFinalUseCase,
            confirmRegistrationUseCase,
            permissionSupport()
        );
    }

    private RecruitingInterviewGraphQlController interviewController() {
        return new RecruitingInterviewGraphQlController(
            getEvaluationUseCase,
            getApplicationQueryUseCase,
            getFormQueryUseCase,
            assignInterviewUseCase,
            skipInterviewUseCase,
            findScheduleCandidatesUseCase,
            sendInterviewGuideUseCase,
            saveEvaluationUseCase,
            submitEvaluationUseCase,
            permissionSupport()
        );
    }

    private RecruitingGraphQlPermissionSupport permissionSupport() {
        return new RecruitingGraphQlPermissionSupport(checkPermissionUseCase, new CurrentMemberProvider());
    }

    private static void authenticateRequester() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }
}
