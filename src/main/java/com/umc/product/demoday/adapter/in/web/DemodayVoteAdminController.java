package com.umc.product.demoday.adapter.in.web;

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

import com.umc.product.demoday.adapter.in.web.dto.request.ChangeDemodayVoteStatusRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayAdminVoteListResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayVoteStatusResponse;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayVoteStatusUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteStatusInfo;
import com.umc.product.demoday.application.port.in.query.ListDemodayAdminVoteUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayAdminVoteListInfo;
import com.umc.product.demoday.application.port.in.query.dto.ListDemodayAdminVoteQuery;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "데모데이 투표 관리자",
    description = "해당 기수 총괄단 또는 SUPER_ADMIN이 데모데이 투표를 관리하는 API"
)
@Validated
@RestController
@RequestMapping("/api/v1/demoday/admin/polls/{pollId}/votes")
@RequiredArgsConstructor
public class DemodayVoteAdminController {

    private final ListDemodayAdminVoteUseCase listDemodayAdminVoteUseCase;
    private final ChangeDemodayVoteStatusUseCase changeDemodayVoteStatusUseCase;

    @Operation(
        operationId = "getAdminVotes",
        summary = "투표 기록 목록 조회",
        description = """
            최신 표부터 voteId 내림차순으로 조회합니다. 무효 처리된 표도 복구할 수 있도록 목록에 남습니다.
            boothId와 participantName을 함께 전달하면 AND로 결합하며, participantName은 회원명만 검색합니다.
            외부 방문자는 이름을 저장하지 않으므로 이름으로 검색할 수 없습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
        @ApiResponse(responseCode = "403", description = "해당 기수의 데모데이 관리자 권한이 없음"),
        @ApiResponse(responseCode = "404", description = "Poll을 찾을 수 없음")
    })
    @GetMapping
    public DemodayAdminVoteListResponse listVotes(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal principal,
        @Parameter(description = "투표 기록을 조회할 Poll ID", example = "101")
        @PathVariable("pollId") Long pollId,
        @Parameter(description = "이전 응답의 nextCursor. 생략하면 첫 페이지입니다.", example = "9000")
        @RequestParam(required = false) Long cursor,
        @Parameter(description = "페이지 크기", example = "50")
        @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
        @Parameter(description = "투표 대상 부스 ID 필터", example = "1")
        @RequestParam(required = false) Long boothId,
        @Parameter(description = "회원명 부분 일치 필터. 외부 방문자는 검색 대상이 아닙니다.", example = "재원")
        @RequestParam(required = false) String participantName
    ) {
        DemodayAdminVoteListInfo demodayAdminVoteListInfo = listDemodayAdminVoteUseCase.listVotes(
            new ListDemodayAdminVoteQuery(
                pollId,
                principal.getMemberId(),
                cursor,
                size,
                boothId,
                participantName
            ));

        return DemodayAdminVoteListResponse.from(demodayAdminVoteListInfo);
    }

    @Operation(
        operationId = "revokeAdminVote",
        summary = "특정 표 무효화",
        description = """
            표를 삭제하지 않고 revokedAt을 기록해 대시보드 집계에서 제외합니다.
            투표 진행 중에도 수행할 수 있으며 무효화된 참여자에게 재투표 기회를 주지는 않습니다.
            행위자·Poll·표 ID·사유는 감사 로그에 기록됩니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "무효화 성공"),
        @ApiResponse(responseCode = "400", description = "사유 누락 또는 길이 위반"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
        @ApiResponse(responseCode = "403", description = "해당 기수의 데모데이 관리자 권한이 없음"),
        @ApiResponse(responseCode = "404", description = "Poll 또는 해당 Poll의 표를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "이미 무효 처리된 표(DEMODAY-0403)")
    })
    @PostMapping("/{voteId}/revocation")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayVoteStatusResponse revokeVote(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal principal,
        @PathVariable("pollId") Long pollId,
        @Parameter(description = "무효화할 표 ID", example = "9000")
        @PathVariable("voteId") Long voteId,
        @Valid @RequestBody ChangeDemodayVoteStatusRequest request
    ) {
        DemodayVoteStatusInfo revoke = changeDemodayVoteStatusUseCase.revoke(
            request.toCommand(pollId, voteId, principal.getMemberId()));

        return DemodayVoteStatusResponse.from(revoke);
    }

    @Operation(
        operationId = "restoreAdminVote",
        summary = "무효 처리된 표의 무효 해제",
        description = """
            revokedAt을 제거해 참여자가 원래 선택했던 부스의 집계에 표를 다시 포함합니다.
            새 표를 만들지 않으며 VALID 상태인 표에는 수행할 수 없습니다.
            행위자·Poll·표 ID·사유는 감사 로그에 기록됩니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "무효 해제 성공"),
        @ApiResponse(responseCode = "400", description = "사유 누락 또는 길이 위반"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 요청"),
        @ApiResponse(responseCode = "403", description = "해당 기수의 데모데이 관리자 권한이 없음"),
        @ApiResponse(responseCode = "404", description = "Poll 또는 해당 Poll의 표를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "무효 처리되지 않은 표(DEMODAY-0415)")
    })
    @PostMapping("/{voteId}/restoration")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayVoteStatusResponse restoreVote(
        @Parameter(hidden = true) @CurrentMember MemberPrincipal principal,
        @PathVariable("pollId") Long pollId,
        @Parameter(description = "무효 해제할 표 ID", example = "9000")
        @PathVariable("voteId") Long voteId,
        @Valid @RequestBody ChangeDemodayVoteStatusRequest request
    ) {
        DemodayVoteStatusInfo restore = changeDemodayVoteStatusUseCase.restore(
            request.toCommand(pollId, voteId, principal.getMemberId()));

        return DemodayVoteStatusResponse.from(restore);
    }
}
