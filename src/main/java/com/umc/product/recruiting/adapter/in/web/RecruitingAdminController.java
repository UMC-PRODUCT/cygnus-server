package com.umc.product.recruiting.adapter.in.web;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.RecruitingDocumentDecisionRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.SkipRecruitingInterviewRequest;
import com.umc.product.recruiting.adapter.in.web.dto.request.UpsertRecruitingApplicationFormRequest;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingDecisionHistoryPageResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingIdResponse;
import com.umc.product.recruiting.adapter.in.web.dto.response.RecruitingStatusSummaryResponse;
import com.umc.product.recruiting.application.port.in.command.CancelRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.ConfirmRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingDocumentUseCase;
import com.umc.product.recruiting.application.port.in.command.DecideRecruitingFinalUseCase;
import com.umc.product.recruiting.application.port.in.command.PrepareRecruitingRegistrationUseCase;
import com.umc.product.recruiting.application.port.in.command.SkipRecruitingInterviewUseCase;
import com.umc.product.recruiting.application.port.in.command.UpsertRecruitingApplicationFormUseCase;
import com.umc.product.recruiting.application.port.in.command.dto.CancelRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.ConfirmRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.command.dto.PrepareRecruitingRegistrationCommand;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.ExportRecruitingDecisionHistoryCsvUseCase;
import com.umc.product.recruiting.application.port.in.query.GetRecruitingApplicationQueryUseCase;
import com.umc.product.recruiting.application.port.in.query.SearchRecruitingDecisionHistoryUseCase;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingDecisionHistorySearchQuery;
import com.umc.product.recruiting.application.port.in.query.dto.RecruitingStatusSummaryQuery;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionHistorySortOrder;
import com.umc.product.recruiting.domain.enums.RecruitingDecisionResult;

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

    private final UpsertRecruitingApplicationFormUseCase upsertFormUseCase;
    private final DecideRecruitingDocumentUseCase decideDocumentUseCase;
    private final DecideRecruitingFinalUseCase decideFinalUseCase;
    private final SkipRecruitingInterviewUseCase skipInterviewUseCase;
    private final PrepareRecruitingRegistrationUseCase prepareRegistrationUseCase;
    private final CancelRecruitingRegistrationUseCase cancelRegistrationUseCase;
    private final ConfirmRecruitingRegistrationUseCase confirmRegistrationUseCase;
    private final GetRecruitingApplicationQueryUseCase getApplicationQueryUseCase;
    private final ExportRecruitingCsvUseCase exportRecruitingCsvUseCase;
    private final SearchRecruitingDecisionHistoryUseCase searchDecisionHistoryUseCase;
    private final ExportRecruitingDecisionHistoryCsvUseCase exportDecisionHistoryCsvUseCase;

    @PutMapping("/seasons/{seasonId}/rounds/{roundId}/form")
    @CheckAccess(resourceType = ResourceType.RECRUITMENT, resourceId = "#seasonId", permission = PermissionType.WRITE)
    @Operation(
        operationId = "RECRUITING-ADMIN-021",
        summary = "지원 Form 전체 구조 Upsert",
        description = "Round가 DRAFT일 때 section, question, option과 COMMON/TRACK 정책을 하나의 요청으로 동기화합니다."
    )
    public RecruitingIdResponse upsertForm(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long seasonId,
        @PathVariable @Positive Long roundId,
        @Valid @RequestBody UpsertRecruitingApplicationFormRequest request
    ) {
        return RecruitingIdResponse.from(upsertFormUseCase.upsert(
            request.toCommand(seasonId, roundId, memberId(memberPrincipal))
        ));
    }

    @PatchMapping("/applications/{applicationId}/document-decision")
    @Operation(
        operationId = "RECRUITING-ADMIN-061",
        summary = "서류 합불 결정",
        description = "학교 회장단 또는 중앙 운영진 CurrentMember 권한으로 서류 합불을 결정합니다. 합격 시 면접 차수는 일정 요청을 자동 생성하고, 면접 미진행 차수는 면접 생략 상태로 전환합니다."
    )
    public void decideDocument(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody RecruitingDocumentDecisionRequest request
    ) {
        decideDocumentUseCase.decideDocument(request.toCommand(applicationId, memberId(memberPrincipal)));
    }

    @PostMapping("/applications/{applicationId}/interview/skip")
    @Operation(
        operationId = "RECRUITING-ADMIN-062",
        summary = "면접 생략",
        description = "CurrentMember 운영 권한으로 서류 합격 지원서를 면접 생략 상태로 전환합니다. 권한은 use case가 실제 지원서 소속으로 검증합니다."
    )
    public void skipInterview(
        @Parameter(hidden = true)
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable @Positive Long applicationId,
        @Valid @RequestBody SkipRecruitingInterviewRequest request
    ) {
        skipInterviewUseCase.skip(request.toCommand(applicationId, memberId(memberPrincipal)));
    }

    @PatchMapping("/applications/{applicationId}/final-decision")
    @Operation(
        operationId = "RECRUITING-ADMIN-063",
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
        operationId = "RECRUITING-ADMIN-071",
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
        operationId = "RECRUITING-ADMIN-072",
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
        operationId = "RECRUITING-ADMIN-073",
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
    @Operation(
        operationId = "RECRUITING-ADMIN-081",
        summary = "지원 현황 요약 조회",
        description = "운영진이 여러 학교와 Round의 지원서 상태별 집계를 조회합니다. 모집 목록이 아니라 현황 대시보드용 집계 API입니다."
    )
    public RecruitingStatusSummaryResponse getSummary(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam @Positive Long gisuId,
        @RequestParam(required = false) List<@Positive Long> schoolIds,
        @RequestParam(required = false) List<@Positive Long> roundIds,
        @RequestParam(required = false) String schoolName
    ) {
        return RecruitingStatusSummaryResponse.from(
            getApplicationQueryUseCase.getStatusSummary(RecruitingStatusSummaryQuery.builder()
                .gisuId(gisuId)
                .schoolIds(schoolIds == null ? Set.of() : Set.copyOf(schoolIds))
                .roundIds(roundIds == null ? Set.of() : Set.copyOf(roundIds))
                .schoolName(schoolName)
                .requesterMemberId(memberId(memberPrincipal))
                .build())
        );
    }

    @GetMapping("/statistics.csv")
    @Operation(
        operationId = "RECRUITING-ADMIN-082",
        summary = "지원 현황 CSV 다운로드",
        description = "지원서 본문과 원본 이메일을 제외한 학교별 지원 현황 CSV를 다운로드합니다."
    )
    public ResponseEntity<byte[]> exportCsv(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam @Positive Long gisuId,
        @RequestParam(required = false) @Positive Long schoolId
    ) {
        byte[] csv = exportRecruitingCsvUseCase.exportSummaryCsv(gisuId, schoolId, memberId(memberPrincipal));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("recruiting-statistics.csv")
                    .build()
                    .toString())
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv);
    }

    @GetMapping("/decision-histories")
    @Operation(
        operationId = "RECRUITING-ADMIN-091",
        summary = "평가 이력 조회",
        description = "교내 회장단·중앙 총괄단의 서류/최종 판정 이력을 감사 목적으로 조회합니다. "
            + "SUPER_ADMIN과 기수 내 중앙운영사무국 구성원만 접근할 수 있으며, "
            + "담당자별 정렬 시 담당자의 최초 판정 시각 순으로 그룹을 배치하고 그룹 내부는 요청한 정렬 순서를 따릅니다."
    )
    public RecruitingDecisionHistoryPageResponse searchDecisionHistories(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam @Positive Long gisuId,
        @RequestParam(required = false) @Positive Long chapterId,
        @RequestParam(required = false) @Positive Long schoolId,
        @RequestParam(required = false) List<ChallengerTrack> tracks,
        @RequestParam(required = false) List<RecruitingDecisionResult> results,
        @RequestParam(required = false) String searchName,
        @RequestParam(required = false) RecruitingDecisionHistorySortOrder sort,
        @RequestParam(required = false, defaultValue = "false") boolean groupByDecider,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return RecruitingDecisionHistoryPageResponse.from(searchDecisionHistoryUseCase.search(
            toDecisionHistoryQuery(
                memberPrincipal, gisuId, chapterId, schoolId, tracks, results, searchName, sort, groupByDecider,
                pageable
            )
        ));
    }

    @GetMapping("/decision-histories.csv")
    @Operation(
        operationId = "RECRUITING-ADMIN-092",
        summary = "평가 이력 CSV 다운로드",
        description = "평가 이력 조회와 같은 조건으로 원문 이메일과 실명을 제외한 CSV를 다운로드합니다."
    )
    public ResponseEntity<byte[]> exportDecisionHistoryCsv(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @RequestParam @Positive Long gisuId,
        @RequestParam(required = false) @Positive Long chapterId,
        @RequestParam(required = false) @Positive Long schoolId,
        @RequestParam(required = false) List<ChallengerTrack> tracks,
        @RequestParam(required = false) List<RecruitingDecisionResult> results,
        @RequestParam(required = false) String searchName,
        @RequestParam(required = false) RecruitingDecisionHistorySortOrder sort,
        @RequestParam(required = false, defaultValue = "false") boolean groupByDecider
    ) {
        byte[] csv = exportDecisionHistoryCsvUseCase.exportCsv(toDecisionHistoryQuery(
            memberPrincipal, gisuId, chapterId, schoolId, tracks, results, searchName, sort, groupByDecider, null
        ));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename("recruiting-decision-histories.csv")
                    .build()
                    .toString())
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(csv);
    }

    private RecruitingDecisionHistorySearchQuery toDecisionHistoryQuery(
        MemberPrincipal memberPrincipal,
        Long gisuId,
        Long chapterId,
        Long schoolId,
        List<ChallengerTrack> tracks,
        List<RecruitingDecisionResult> results,
        String searchName,
        RecruitingDecisionHistorySortOrder sort,
        boolean groupByDecider,
        Pageable pageable
    ) {
        return RecruitingDecisionHistorySearchQuery.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .schoolId(schoolId)
            .tracks(tracks == null ? Set.of() : Set.copyOf(tracks))
            .results(results == null ? Set.of() : Set.copyOf(results))
            .searchName(searchName)
            .sortOrder(sort)
            .groupByDecider(groupByDecider)
            .requesterMemberId(memberId(memberPrincipal))
            .pageable(pageable)
            .build();
    }

    private Long memberId(MemberPrincipal memberPrincipal) {
        return memberPrincipal.getMemberId();
    }
}
