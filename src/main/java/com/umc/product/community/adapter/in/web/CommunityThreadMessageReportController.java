package com.umc.product.community.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.community.adapter.in.web.dto.request.ReportCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadMessageReportPageResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadMessageReportReceiptResponse;
import com.umc.product.community.adapter.in.web.validation.PositiveDecimalId;
import com.umc.product.community.application.port.in.command.thread.report.ReportCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.query.thread.report.SearchCommunityThreadMessageReportsUseCase;
import com.umc.product.community.application.port.in.query.thread.report.dto.SearchCommunityThreadMessageReportsQuery;
import com.umc.product.community.domain.enums.ReportReason;
import com.umc.product.community.domain.enums.ReportStatus;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Validated
@Tag(name = "Community | Thread Message Report", description = "Thread message 신고와 admin inbox API")
public class CommunityThreadMessageReportController {

    private final ReportCommunityThreadMessageUseCase reportMessageUseCase;
    private final SearchCommunityThreadMessageReportsUseCase searchReportsUseCase;

    @PostMapping("/messages/{messageId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "COMMUNITY-THREAD-REPORT-001", summary = "Thread message 신고")
    public CommunityThreadMessageReportReceiptResponse reportMessage(
        @PathVariable @PositiveDecimalId String messageId,
        @Valid @RequestBody ReportCommunityThreadMessageRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMessageReportReceiptResponse.from(reportMessageUseCase.report(
            request.toCommand(CommunityWebNumbers.id(messageId), principal.getMemberId())
        ));
    }

    @GetMapping("/admin/thread-message-reports")
    @Operation(operationId = "COMMUNITY-THREAD-REPORT-101", summary = "Thread message 신고 admin inbox")
    public CommunityThreadMessageReportPageResponse searchReports(
        @RequestParam(defaultValue = "PENDING") ReportStatus status,
        @RequestParam(required = false) ReportReason reason,
        @RequestParam(required = false) @PositiveDecimalId String threadId,
        @RequestParam(required = false) @PositiveDecimalId String reporterId,
        @RequestParam(defaultValue = "0") @Min(0) int offset,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMessageReportPageResponse.from(searchReportsUseCase.search(
            new SearchCommunityThreadMessageReportsQuery(
                principal.getMemberId(),
                status,
                reason,
                CommunityWebNumbers.optionalId(threadId),
                CommunityWebNumbers.optionalId(reporterId),
                offset,
                limit
            )
        ));
    }
}
