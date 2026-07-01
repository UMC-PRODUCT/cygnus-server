package com.umc.product.recruiting.adapter.in.web;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingRoundRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.CreateRecruitingSeasonRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.LinkRecruitingApplicationFormRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingRoundStatusRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpdateRecruitingSeasonStatusRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingStatusSummaryResponse;
import com.umc.product.recruiting.application.port.in.command.CloseRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingRoundUseCase;
import com.umc.product.recruiting.application.port.in.command.CreateRecruitingSeasonUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.LinkRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.PublishRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingRoundStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.UpdateRecruitingSeasonStatusUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CloseRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PublishRecruitingApplicationFormCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/recruiting")
@RequiredArgsConstructor
public class RecruitingAdminController {

    private final CreateRecruitingSeasonUseCase createSeasonUseCase;
    private final UpdateRecruitingSeasonStatusUseCase updateSeasonStatusUseCase;
    private final CreateRecruitingRoundUseCase createRoundUseCase;
    private final UpdateRecruitingRoundStatusUseCase updateRoundStatusUseCase;
    private final LinkRecruitingApplicationFormUseCase linkFormUseCase;
    private final PublishRecruitingApplicationFormUseCase publishFormUseCase;
    private final CloseRecruitingApplicationFormUseCase closeFormUseCase;
    private final DecideRecruitingDocumentUseCase decideDocumentUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;

    @PostMapping("/seasons")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.WRITE)
    public RecruitingIdResponse createSeason(@Valid @RequestBody CreateRecruitingSeasonRequest request) {
        return RecruitingIdResponse.from(createSeasonUseCase.createSeason(request.toCommand()));
    }

    @PatchMapping("/seasons/{seasonId}/status")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void updateSeasonStatus(
        @PathVariable Long seasonId,
        @Valid @RequestBody UpdateRecruitingSeasonStatusRequest request
    ) {
        updateSeasonStatusUseCase.updateSeasonStatus(request.toCommand(seasonId));
    }

    @PostMapping("/seasons/{seasonId}/rounds")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.WRITE)
    public RecruitingIdResponse createRound(
        @PathVariable Long seasonId,
        @Valid @RequestBody CreateRecruitingRoundRequest request
    ) {
        return RecruitingIdResponse.from(createRoundUseCase.createRound(request.toCommand(seasonId)));
    }

    @PatchMapping("/seasons/{seasonId}/rounds/{roundId}/status")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void updateRoundStatus(
        @PathVariable Long seasonId,
        @PathVariable Long roundId,
        @Valid @RequestBody UpdateRecruitingRoundStatusRequest request
    ) {
        updateRoundStatusUseCase.updateRoundStatus(request.toCommand(roundId));
    }

    @PostMapping("/seasons/{seasonId}/rounds/{roundId}/forms")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.WRITE)
    public RecruitingIdResponse linkForm(
        @PathVariable Long seasonId,
        @PathVariable Long roundId,
        @Valid @RequestBody LinkRecruitingApplicationFormRequest request
    ) {
        return RecruitingIdResponse.from(linkFormUseCase.link(request.toCommand(roundId)));
    }

    @PostMapping("/seasons/{seasonId}/forms/{applicationFormId}/publish")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void publishForm(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationFormId
    ) {
        publishFormUseCase.publish(PublishRecruitingApplicationFormCommand.builder()
            .applicationFormId(applicationFormId)
            .requesterMemberId(memberId(memberPrincipal))
            .build());
    }

    @PostMapping("/seasons/{seasonId}/forms/{applicationFormId}/close")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.EDIT)
    public void closeForm(
        @PathVariable Long seasonId,
        @PathVariable Long applicationFormId
    ) {
        closeFormUseCase.close(CloseRecruitingApplicationFormCommand.builder()
            .applicationFormId(applicationFormId)
            .build());
    }

    @PatchMapping("/seasons/{seasonId}/applications/{applicationId}/document-decision")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.APPROVE)
    public void decideDocument(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitingDecisionRequest request
    ) {
        decideDocumentUseCase.decideDocument(request.toDocumentCommand(applicationId, memberId(memberPrincipal)));
    }

    @PatchMapping("/seasons/{seasonId}/applications/{applicationId}/final-decision")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.APPROVE)
    public void decideFinal(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId,
        @Valid @RequestBody RecruitingDecisionRequest request
    ) {
        decideFinalUseCase.decideFinal(request.toFinalCommand(applicationId, memberId(memberPrincipal)));
    }

    @PostMapping("/seasons/{seasonId}/applications/{applicationId}/registration-confirm")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.MANAGE)
    public void confirmRegistration(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable Long seasonId,
        @PathVariable Long applicationId
    ) {
        confirmRegistrationUseCase.confirmRegistration(ConfirmRecruitingRegistrationCommand.builder()
            .applicationId(applicationId)
            .executorMemberId(memberId(memberPrincipal))
            .build());
    }

    @GetMapping("/summary")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.MANAGE)
    public RecruitingStatusSummaryResponse getSummary(
        @RequestParam Long gisuId,
        @RequestParam Long schoolId
    ) {
        return RecruitingStatusSummaryResponse.from(getApplicationQueryUseCase.getStatusSummary(gisuId, schoolId));
    }

    @GetMapping("/statistics.csv")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, permission = PermissionType.MANAGE)
    public ResponseEntity<byte[]> exportCsv(
        @RequestParam Long gisuId,
        @RequestParam Long schoolId
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
        return memberPrincipal == null ? null : memberPrincipal.getMemberId();
    }
}
