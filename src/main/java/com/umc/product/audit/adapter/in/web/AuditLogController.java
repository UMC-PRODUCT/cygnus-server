package com.umc.product.audit.adapter.in.web;

import java.time.Instant;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.audit.application.port.in.query.GetAuditLogUseCase;
import com.umc.product.audit.application.port.in.query.dto.AuditLogInfo;
import com.umc.product.audit.application.port.in.query.dto.SearchAuditLogQuery;
import com.umc.product.audit.domain.AuditAction;
import com.umc.product.audit.domain.AuditOutcome;
import com.umc.product.audit.domain.AuditSource;
import com.umc.product.authorization.adapter.in.aspect.CheckAccess;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.global.exception.constant.Domain;
import com.umc.product.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/audit/admin/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit | 감사 로그 조회", description = "관리자가 감사 로그를 검색합니다.")
public class AuditLogController {

    private final GetAuditLogUseCase getAuditLogUseCase;

    @Operation(operationId = "AUDIT-001", summary = "감사 로그 검색")
    @CheckAccess(
        resourceType = ResourceType.AUDIT,
        permission = PermissionType.READ,
        message = "감사 로그는 중앙운영사무국 국원만 조회할 수 있어요. 필요한 권한이 있다면 운영진에게 문의해주세요."
    )
    @GetMapping
    public ApiResponse<Page<AuditLogInfo>> search(
        @Parameter(description = "감사 로그 도메인 enum")
        @RequestParam(required = false) Domain domain,
        @Parameter(description = "감사 로그 액션 enum")
        @RequestParam(required = false) AuditAction action,
        @Parameter(description = "행위자 회원 ID")
        @RequestParam(required = false) Long actorMemberId,
        @Parameter(description = "조회 시작 시각(ISO-8601 Instant, inclusive)")
        @RequestParam(required = false) Instant from,
        @Parameter(description = "조회 종료 시각(ISO-8601 Instant, inclusive)")
        @RequestParam(required = false) Instant to,
        @Parameter(description = "감사 대상 타입. 정확히 일치하는 값만 조회")
        @RequestParam(required = false) String targetType,
        @Parameter(description = "감사 대상 식별자 문자열. 숫자 검증 없이 정확히 일치하는 값만 조회")
        @RequestParam(required = false) String targetId,
        @Parameter(description = "감사 결과 enum")
        @RequestParam(required = false) AuditOutcome outcome,
        @Parameter(description = "감사 로그 출처 enum")
        @RequestParam(required = false) AuditSource source,
        @Parameter(description = "요청 식별자. 정확히 일치하는 값만 조회")
        @RequestParam(required = false) String requestId,
        @Parameter(description = "분산 추적 식별자. 정확히 일치하는 값만 조회")
        @RequestParam(required = false) String traceId,
        @PageableDefault(size = 20) @ParameterObject Pageable pageable
    ) {
        SearchAuditLogQuery query = new SearchAuditLogQuery(
            domain,
            action,
            actorMemberId,
            from,
            to,
            targetType,
            targetId,
            outcome,
            source,
            requestId,
            traceId
        );
        Page<AuditLogInfo> result = getAuditLogUseCase.search(query, pageable);

        return ApiResponse.onSuccess(result);
    }
}
