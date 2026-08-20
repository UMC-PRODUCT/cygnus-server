package com.umc.product.demoday.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.response.DemodayTestDataResetApiResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayTestDataResetResponse;
import com.umc.product.demoday.application.port.in.command.ResetDemodayTestDataUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayTestDataResetInfo;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "데모데이 테스트 데이터 관리자",
    description = "비운영 환경에서 SUPER_ADMIN이 데모데이 테스트 데이터를 초기화하는 API"
)
@RestController
@RequestMapping("/api/v1/demoday/admin/test-data")
@Profile("!prod")
@ConditionalOnProperty(prefix = "demoday.test-data-reset", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemodayTestDataAdminController {

    private final ResetDemodayTestDataUseCase resetDemodayTestDataUseCase;

    @Operation(
        operationId = "resetDemodayTestData",
        summary = "데모데이 테스트 데이터 전체 초기화 (SUPER_ADMIN 전용)",
        description = """
            **이 API는 비운영 환경에서만 활성화되며 SUPER_ADMIN만 호출할 수 있습니다.**
            데모데이 Poll, 부스, 외부인 입장 코드, 스탬프, 표를 외래 키에 안전한 순서로 모두 삭제합니다.

            전체 삭제는 하나의 트랜잭션에서 실행되므로 중간에 실패하면 전부 롤백됩니다.
            이미 데이터가 없는 상태에서 다시 호출해도 성공하며, 각 응답 필드는 `0`을 반환합니다.
            공통 응답의 `result`는 데이터 종류별로 실제 삭제된 행 수를 반환합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "초기화 성공",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = DemodayTestDataResetApiResponse.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
        @ApiResponse(responseCode = "403", description = "SUPER_ADMIN 권한이 없음")
    })
    @DeleteMapping
    public DemodayTestDataResetResponse reset(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal principal
    ) {
        DemodayTestDataResetInfo info = resetDemodayTestDataUseCase.reset(requireMemberId(principal));
        return DemodayTestDataResetResponse.from(info);
    }

    private Long requireMemberId(MemberPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("회원 인증이 필요한 데모데이 관리자 API입니다.");
        }
        return principal.getMemberId();
    }
}
