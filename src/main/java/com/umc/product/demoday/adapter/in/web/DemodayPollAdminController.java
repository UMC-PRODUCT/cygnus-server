package com.umc.product.demoday.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.request.ChangeDemodayPollStatusRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.CreateDemodayPollRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.GenerateDemodayEntryCodesRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.CreateDemodayEntryCodeResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.CreateDemodayPollResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayDashboardResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayVoteQrResponse;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayEntryCodeUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;
import com.umc.product.demoday.application.port.in.query.GetDemodayDashboardUseCase;
import com.umc.product.demoday.application.port.in.query.GetDemodayVoteQrUseCase;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "데모데이 투표 관리자",
    description = "해당 기수 총괄단 또는 SUPER_ADMIN이 데모데이 투표를 관리하는 API"
)
@RestController
@RequestMapping("/api/v1/demoday/admin/polls")
@RequiredArgsConstructor
public class DemodayPollAdminController {

    private final CreateDemodayPollUseCase createDemodayPollUseCase;
    private final ChangeDemodayPollStatusUseCase changeDemodayPollStatusUseCase;
    private final CreateDemodayEntryCodeUseCase createDemodayEntryCodeUseCase;
    private final GetDemodayVoteQrUseCase getDemodayVoteQrUseCase;
    private final GetDemodayDashboardUseCase getDemodayDashboardUseCase;

    @Operation(
        operationId = "createDemodayPoll",
        summary = "데모데이 투표 행사 생성",
        description = """
                데모데이 투표를 READY 상태로 생성합니다.

            요청자는 요청한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
            opensAt과 closesAt은 실제 투표 가능 시간이며, 시간이 되어도 운영 상태가 자동으로 바뀌지는 않습니다.
            """
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDemodayPollResponse createPoll(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateDemodayPollRequest request) {

        Long pollId = createDemodayPollUseCase.create(request.toCommand(memberPrincipal.getMemberId()));

        return CreateDemodayPollResponse.from(pollId);
    }

    @Operation(
        operationId = "changeDemodayPollStatus",
        summary = "데모데이 투표 상태 변경",
        description = """
                데모데이 투표의 운영 상태를 READY → OPEN → CLOSED 순서로 변경합니다.

                요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
                상태 변경 요청에는 OPEN과 CLOSED만 사용할 수 있으며, 허용되지 않은 상태 전이 또는 이미 같은 상태라면 409 응답을 반환합니다.
                운영 상태는 opensAt, closesAt과 독립적이므로 기간이 지나도 자동으로 변경되지 않습니다.
            """
    )
    @PatchMapping("/{pollId}/status")
    public void changePollStatus(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "상태를 변경할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId,
        @Valid @RequestBody ChangeDemodayPollStatusRequest request) {

        changeDemodayPollStatusUseCase.changeStatus(request.toCommand(pollId, memberPrincipal.getMemberId()));
    }

    @PostMapping("/{pollId}/entry-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateDemodayEntryCodeResponse createEntryCodes(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal principal,
        @PathVariable("pollId") Long pollId,
        @Valid @RequestBody GenerateDemodayEntryCodesRequest request
    ) {

        CreateDemodayEntryCodesInfo demodayEntryCodesInfo = createDemodayEntryCodeUseCase.create(
            principal.getMemberId(), request.toCommand(pollId));

        return CreateDemodayEntryCodeResponse.from(demodayEntryCodesInfo);

    }

    @Operation(
        operationId = "getAdminVoteQr",
        summary = "현재 시간 구간의 INFO 투표 인증 QR 조회",
        description = """
            **이 API는 호출할 때마다 새 QR을 생성하는 명령이 아닙니다.**
            현재 시간 구간에서 유효한 INFO 투표 인증 QR을 조회합니다.

            INFO QR credential은 서버가 발급한 토큰이며 1시간마다 변경됩니다.
            같은 유효 구간에서는 여러 운영자가 조회하거나 화면을 새로고침해도
            논리적으로 동일한 credential을 반환합니다.

            `qrValue`는 FE가 추가로 조립하지 않고 그대로 QR 이미지로 렌더링할 수 있는 완성된 값입니다.

            요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 하며,
            투표가 OPEN 상태이고 투표 기간 안일 때만 조회할 수 있습니다(404 DEMODAY-0111).
            """
    )
    @GetMapping("/{pollId}/vote-qr")
    public DemodayVoteQrResponse getVoteQr(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "INFO QR을 조회할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId) {

        return DemodayVoteQrResponse.from(getDemodayVoteQrUseCase.get(pollId, memberPrincipal.getMemberId()));
    }

    @Operation(
        operationId = "getAdminDashboard",
        summary = "Poll 대시보드 통계 스냅샷 조회",
        description = """
            요약 지표·투표 랭킹·스탬프 히트맵을 **하나의 스냅샷으로** 반환합니다.
            세 가지 모두 같은 Poll을 기준으로 하고, 데이터 규모가 부스 수 수준이며,
            같은 화면에서 비슷한 주기로 갱신되고, 같은 권한을 쓰기 때문에 API를 분리하지 않았습니다.

            FE는 WebSocket을 쓰지 않고 이 API를 폴링합니다. 주기는 FE 운영 설정으로 관리하며,
            이전 요청이 끝나기 전에 같은 요청을 중첩하지 않습니다.

            ### 무효 처리된 데이터의 집계 제외

            `summary.totalVoteCount`, `rankings[].voteCount`, `stampHeatmap[].stampCount`는
            모두 **무효 처리되지 않은 행만 집계합니다.** 이 수치가 시상 근거이므로
            어드민이 부정표를 무효화하면 즉시 수치와 순위에서 빠집니다.

            ### 정렬과 순위

            `rankings`는 `voteCount` 내림차순, 동점이면 `boothCode` 오름차순인 **프로젝트 부스** 목록입니다.
            스탬프 적립 전용인 외부 부스는 랭킹과 `summary.totalVoteCount`에서 제외됩니다.
            `rank`는 표준 경쟁 순위입니다. 동점 부스는 같은 순위를 공유하고, 다음 순위는 동점자 수만큼
            건너뜁니다(1, 2, 2, 4). 화면에 표시할 상위 개수는 FE가 정합니다.

            `summary.boothCount`와 `stampHeatmap`은 프로젝트 부스와 외부 부스를 모두 포함합니다.
            `stampHeatmap`에는 스탬프 수가 `0`인 부스도 포함됩니다.
            부스 구역·좌표는 FE 정적 데이터이므로 서버는 반환하지 않습니다. FE가 `boothId`로 결합하세요.

            요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
            """
    )
    @GetMapping("/{pollId}/dashboard")
    public DemodayDashboardResponse getDashboard(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal,
        @Parameter(description = "대시보드를 조회할 데모데이 투표 ID", required = true, example = "1")
        @PathVariable("pollId") Long pollId) {

        return DemodayDashboardResponse.from(
            getDemodayDashboardUseCase.getDashboard(pollId, memberPrincipal.getMemberId()));
    }
}
