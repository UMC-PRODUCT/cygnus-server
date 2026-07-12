package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingInterviewQuestionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingInterviewQuestionResponse;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingApplicationInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.ManageRecruitingRoundInterviewQuestionUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingApplicationInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.command.dto.DeactivateRecruitingRoundInterviewQuestionCommand;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingInterviewQuestionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Validated
@Tag(name = "Recruiting | 면접 질문 관리", description = "운영진과 면접 평가자가 공통 및 지원서별 면접 질문을 관리합니다.")
@RequiredArgsConstructor
public class RecruitingAdminQuestionController {

    private final ManageRecruitingRoundInterviewQuestionUseCase manageRoundQuestionUseCase;
    private final ManageRecruitingApplicationInterviewQuestionUseCase manageApplicationQuestionUseCase;
    private final GetRecruitingInterviewQuestionUseCase getQuestionUseCase;

    @PostMapping("/rounds/{roundId}/questions")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-001", summary = "공통 면접 질문 생성", description = "차수 공통 면접 질문을 생성합니다.")
    public RecruitingIdResponse createRoundQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody RecruitingInterviewQuestionRequest request
    ) {
        return RecruitingIdResponse.from(manageRoundQuestionUseCase.createRoundQuestion(
            request.toRoundCreateCommand(roundId, actor.getMemberId())
        ));
    }

    @PutMapping("/rounds/{roundId}/questions/{questionId}")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-002", summary = "공통 면접 질문 수정", description = "첫 면접 평가 제출 전 공통 질문을 수정합니다.")
    public void updateRoundQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long questionId,
        @Valid @RequestBody RecruitingInterviewQuestionRequest request
    ) {
        manageRoundQuestionUseCase.updateRoundQuestion(
            request.toRoundUpdateCommand(questionId, roundId, actor.getMemberId())
        );
    }

    @DeleteMapping("/rounds/{roundId}/questions/{questionId}")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-003", summary = "공통 면접 질문 비활성화", description = "첫 면접 평가 제출 전 공통 질문을 비활성화합니다.")
    public void deactivateRoundQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long roundId,
        @PathVariable @Positive Long questionId
    ) {
        manageRoundQuestionUseCase.deactivateRoundQuestion(
            DeactivateRecruitingRoundInterviewQuestionCommand.of(questionId, roundId, actor.getMemberId())
        );
    }

    @GetMapping("/rounds/{roundId}/questions")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-004", summary = "공통 면접 질문 조회", description = "차수의 활성 공통 면접 질문을 순서대로 조회합니다.")
    public List<RecruitingInterviewQuestionResponse> listRoundQuestions(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long roundId
    ) {
        return getQuestionUseCase.listActiveRoundQuestions(roundId, actor.getMemberId())
            .stream()
            .map(RecruitingInterviewQuestionResponse::from)
            .toList();
    }

    @PostMapping("/applications/{applicationId}/questions")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-005", summary = "지원서별 면접 질문 생성", description = "INTERVIEW 평가자가 지원서별 면접 질문을 생성합니다.")
    public RecruitingIdResponse createApplicationQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody RecruitingInterviewQuestionRequest request
    ) {
        return RecruitingIdResponse.from(manageApplicationQuestionUseCase.createApplicationQuestion(
            request.toApplicationCreateCommand(applicationId, actor.getMemberId())
        ));
    }

    @PutMapping("/applications/{applicationId}/questions/{questionId}")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-006", summary = "지원서별 면접 질문 수정", description = "첫 면접 평가 제출 전 지원서별 질문을 수정합니다.")
    public void updateApplicationQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId,
        @PathVariable @Positive Long questionId,
        @Valid @RequestBody RecruitingInterviewQuestionRequest request
    ) {
        manageApplicationQuestionUseCase.updateApplicationQuestion(
            request.toApplicationUpdateCommand(questionId, applicationId, actor.getMemberId())
        );
    }

    @DeleteMapping("/applications/{applicationId}/questions/{questionId}")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-007", summary = "지원서별 면접 질문 비활성화", description = "첫 면접 평가 제출 전 지원서별 질문을 비활성화합니다.")
    public void deactivateApplicationQuestion(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId,
        @PathVariable @Positive Long questionId
    ) {
        manageApplicationQuestionUseCase.deactivateApplicationQuestion(
            DeactivateRecruitingApplicationInterviewQuestionCommand.of(
                questionId,
                applicationId,
                actor.getMemberId()
            )
        );
    }

    @GetMapping("/applications/{applicationId}/questions")
    @Operation(operationId = "RECRUITING-ADMIN-QUESTION-008", summary = "지원서별 면접 질문 조회", description = "지원서의 활성 개별 면접 질문을 순서대로 조회합니다.")
    public List<RecruitingInterviewQuestionResponse> listApplicationQuestions(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal actor,
        @PathVariable @Positive Long applicationId
    ) {
        return getQuestionUseCase.listActiveApplicationQuestions(applicationId, actor.getMemberId())
            .stream()
            .map(RecruitingInterviewQuestionResponse::from)
            .toList();
    }
}
