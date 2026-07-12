package com.umc.product.recruiting.adapter.in.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.LinkRecruitingApplicationFormRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingStatusSummaryResponse;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/recruiting/admin")
@Validated
@Tag(name = "Recruiting | 운영진 관리", description = "운영진이 리크루팅 시즌, 차수, 지원 폼, 합불 결정, 통계를 관리합니다.")
@RequiredArgsConstructor
public class RecruitingAdminController {

    private final LinkRecruitingApplicationFormUseCase linkFormUseCase;
    private final PublishRecruitingApplicationFormUseCase publishFormUseCase;
    private final CloseRecruitingApplicationFormUseCase closeFormUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    private final CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;

    @PostMapping("/seasons/{seasonId}/rounds/{roundId}/forms")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.WRITE)
    @Operation(
        operationId = "RECRUITING-ADMIN-005",
        summary = "지원 폼 연결",
        description = "모집 차수에 form 엔진의 지원 폼 하나를 연결합니다."
    )
    public RecruitingIdResponse linkForm(
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody LinkRecruitingApplicationFormRequest request
    ) {
        return RecruitingIdResponse.from(linkFormUseCase.link(request.toCommand(seasonId, roundId)));
    }

    @PostMapping("/seasons/{seasonId}/forms/{applicationFormId}/publish")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-006",
        summary = "지원 폼 게시",
        description = "연결된 지원 폼을 지원자에게 공개합니다."
    )
    public void publishForm(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long applicationFormId
    ) {
        publishFormUseCase.publish(PublishRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .applicationFormId(applicationFormId)
            .requesterMemberId(memberId(memberPrincipal))
            .build());
    }

    @PostMapping("/seasons/{seasonId}/forms/{applicationFormId}/close")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    @Operation(
        operationId = "RECRUITING-ADMIN-007",
        summary = "지원 폼 마감",
        description = "공개된 지원 폼을 수동으로 마감합니다."
    )
    public void closeForm(
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long applicationFormId
    ) {
        closeFormUseCase.close(CloseRecruitingApplicationFormCommand.builder()
            .seasonId(seasonId)
            .applicationFormId(applicationFormId)
            .build());
    }

    @PatchMapping("/applications/{applicationId}/final-decision")
    @Operation(
        operationId = "RECRUITING-ADMIN-009",
        summary = "최종 합불 결정",
        description = "학교 회장단 또는 중앙 운영진 CurrentMember 권한으로 최종 합불을 결정합니다. 권한은 use case가 실제 지원서 소속으로 검증합니다."
    )
    public void decideFinal(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody RecruitingDecisionRequest request
    ) {
        decideFinalUseCase.decideFinal(request.toFinalCommand(applicationId, memberId(memberPrincipal)));
    }

    @PostMapping("/applications/{applicationId}/registration/ready")
    @Operation(
        operationId = "RECRUITING-ADMIN-010",
        summary = "등록 준비",
        description = "중앙 운영진 CurrentMember 권한으로 최종 합격자의 트랙 쿼터를 예약해 READY로 전환합니다."
    )
    public void prepareRegistration(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId
    ) {
        prepareRegistrationUseCase.prepareRegistration(
            PrepareRecruitingRegistrationCommand.of(applicationId, memberId(memberPrincipal))
        );
    }

    @DeleteMapping("/applications/{applicationId}/registration/ready")
    @Operation(
        operationId = "RECRUITING-ADMIN-010A",
        summary = "등록 준비 취소",
        description = "중앙 운영진 CurrentMember 권한으로 READY 예약을 취소하고 쿼터를 반환합니다."
    )
    public void cancelRegistration(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId
    ) {
        cancelRegistrationUseCase.cancelRegistration(
            CancelRecruitingRegistrationCommand.of(applicationId, memberId(memberPrincipal))
        );
    }

    @PostMapping("/applications/{applicationId}/registration/registered")
    @Operation(
        operationId = "RECRUITING-ADMIN-010B",
        summary = "챌린저 등록 확정",
        description = "중앙 운영진 CurrentMember 권한으로 READY 지원자를 REGISTERED로 전환하고 Challenger 등록 use case에 위임합니다."
    )
    public void confirmRegistration(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId
    ) {
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(memberId(memberPrincipal))
            .build());
    }

    @GetMapping("/summary")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.MANAGE)
    @Operation(
        operationId = "RECRUITING-ADMIN-011",
        summary = "지원 현황 요약 조회",
        description = "기수와 학교 기준으로 지원서 상태별 집계와 전체 건수를 조회합니다."
    )
    public RecruitingStatusSummaryResponse getSummary(
        @RequestParam @Positive Long gisuId,
        @RequestParam @Positive Long schoolId
    ) {
        return RecruitingStatusSummaryResponse.from(getApplicationQueryUseCase.getStatusSummary(gisuId, schoolId));
    }

    @GetMapping("/statistics.csv")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.MANAGE)
    @Operation(
        operationId = "RECRUITING-ADMIN-012",
        summary = "지원 현황 CSV 다운로드",
        description = "지원서 본문과 원본 이메일을 제외한 학교별 지원 현황 CSV를 다운로드합니다."
    )
    public ResponseEntity<byte[]> exportCsv(
        @RequestParam @Positive Long gisuId,
        @RequestParam(required = false) @Positive Long schoolId
    ) {
        byte[] csv = exportRecruitingCsvUseCase.exportSummaryCsv(gisuId, schoolId);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("recruiting-statistics.csv")
                    .build()
                    .toString())
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv);
    }

    private Long memberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal.getMemberId();
    }
}
