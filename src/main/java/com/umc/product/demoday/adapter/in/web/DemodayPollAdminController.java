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
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayVoteQrResponse;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayEntryCodeUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.command.dto.CreateDemodayEntryCodesInfo;
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
}
