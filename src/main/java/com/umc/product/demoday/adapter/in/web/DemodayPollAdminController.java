package com.umc.product.demoday.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.request.ChangeDemodayPollStatusRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.CreateDemodayPollRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.CreateDemodayPollResponse;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
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

    @Operation(
        operationId = "createDemodayPoll",
        summary = "데모데이 투표 행사 생성",
        description = """
            데모데이 투표를 CLOSED 상태로 생성합니다.

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
            데모데이 투표의 운영 상태를 OPEN 또는 CLOSED로 변경합니다.

            요청자는 투표가 속한 기수의 총괄단이거나 SUPER_ADMIN이어야 합니다.
            CLOSED 상태의 투표를 다시 OPEN하여 재개할 수 있으며, 이미 같은 상태라면 409 응답을 반환합니다.
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
}
