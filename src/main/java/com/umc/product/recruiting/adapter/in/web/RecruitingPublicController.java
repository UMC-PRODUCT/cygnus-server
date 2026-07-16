package com.umc.product.recruiting.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.annotation.Public;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateAnonymousRecruitingApplicationDraftRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingApplicationCredentialRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SubmitAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateAnonymousRecruitingApplicationRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationCreatedResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationFormResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationFormStructureResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingApplicationResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingPublicApplicationResponse;
import com.umc.product.recruiting.application.port.in.command.CreateAnonymousRecruitingApplicationDraftUseCase;
import com.umc.product.recruiting.application.port.in.command.SubmitAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetAnonymousRecruitingApplicationUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingFormQueryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/public")
@Validated
@Tag(name = "Recruiting | 공개 모집", description = "지원자가 공개 모집 폼과 지원 결과를 조회합니다.")
@RequiredArgsConstructor
public class RecruitingPublicController {

    private final GetRecruitingFormQueryUseCase getRecruitingFormQueryUseCase;
    private final GetAnonymousRecruitingApplicationUseCase getAnonymousApplicationUseCase;
    private final CreateAnonymousRecruitingApplicationDraftUseCase createAnonymousDraftUseCase;
    private final UpdateAnonymousRecruitingApplicationUseCase updateAnonymousApplicationUseCase;
    private final SubmitAnonymousRecruitingApplicationUseCase submitAnonymousApplicationUseCase;

    @GetMapping("/forms")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-001",
        summary = "공개 지원 폼 목록 조회",
        description = "기수와 학교 기준으로 현재 공개된 리크루팅 지원 폼 목록을 조회합니다."
    )
    public List<RecruitingApplicationFormResponse> listPublicForms(
        @RequestParam @Positive Long gisuId,
        @RequestParam @Positive Long schoolId
    ) {
        return getRecruitingFormQueryUseCase.listPublicForms(gisuId, schoolId)
            .stream()
            .map(RecruitingApplicationFormResponse::from)
            .toList();
    }

    @GetMapping("/forms/{applicationFormId}/structure")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-002",
        summary = "지원 Form 구조 조회",
        description = "지원자가 선택한 1·2지망에 필요한 공통 및 트랙 section만 조회합니다."
    )
    public RecruitingApplicationFormStructureResponse getPublicFormStructure(
        @PathVariable @Positive Long applicationFormId,
        @RequestParam ChallengerTrack firstChoice,
        @RequestParam(required = false) ChallengerTrack secondChoice
    ) {
        return RecruitingApplicationFormStructureResponse.from(
            getRecruitingFormQueryUseCase.getPublicFormStructure(applicationFormId, firstChoice, secondChoice)
        );
    }

    @PostMapping("/applications")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-003",
        summary = "익명 지원서 초안 생성",
        description = "개인정보 처리방침 동의를 검증하고 익명 Form 응답과 지원서 초안을 생성합니다. 지원 키는 이 응답에서만 제공합니다."
    )
    public RecruitingApplicationCreatedResponse createAnonymousDraft(
        @Valid @RequestBody CreateAnonymousRecruitingApplicationDraftRequest request
    ) {
        return RecruitingApplicationCreatedResponse.from(createAnonymousDraftUseCase.createAnonymousDraft(request.toCommand()));
    }

    @PostMapping("/applications/lookup")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-004",
        summary = "익명 지원서 조회",
        description = "지원 이메일과 지원 키로 지원서와 Form 답변을 조회합니다. 전형 결과는 각 발표 시각부터 제공합니다."
    )
    public RecruitingPublicApplicationResponse getAnonymousApplication(
        @Valid @RequestBody RecruitingApplicationCredentialRequest request
    ) {
        return RecruitingPublicApplicationResponse.from(
            getAnonymousApplicationUseCase.getByCredential(request.email(), request.applicationKey())
        );
    }

    @PutMapping("/applications")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-005",
        summary = "익명 지원서 수정",
        description = "서류 접수 마감 전 익명 초안 또는 제출 완료 지원서의 기본 정보와 선택 트랙 범위 답변을 수정합니다."
    )
    public RecruitingApplicationResponse updateAnonymousApplication(
        @Valid @RequestBody UpdateAnonymousRecruitingApplicationRequest request
    ) {
        return RecruitingApplicationResponse.from(updateAnonymousApplicationUseCase.updateAnonymous(request.toCommand()));
    }

    @PostMapping("/applications/submit")
    @Public
    @Operation(
        operationId = "RECRUITING-PUBLIC-006",
        summary = "익명 지원서 제출",
        description = "선택한 트랙과 실제 조건부 이동 경로의 필수 문항을 검증한 뒤 익명 지원서를 제출합니다."
    )
    public RecruitingApplicationResponse submitAnonymousApplication(
        @Valid @RequestBody SubmitAnonymousRecruitingApplicationRequest request,
        HttpServletRequest servletRequest
    ) {
        return RecruitingApplicationResponse.from(
            submitAnonymousApplicationUseCase.submitAnonymous(request.toCommand(servletRequest.getRemoteAddr()))
        );
    }

}
