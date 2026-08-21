package com.umc.product.demoday.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.request.CastDemodayVoteRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.CreateDemodayVoteAuthorizationRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.GuestParticipationRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.StampCollectRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayParticipationResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayStampCollectResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayVoteAuthorizationResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayVoteResponse;
import com.umc.product.demoday.adapter.in.web.security.CurrentDemodayParticipant;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantCookieWriter;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantTokenProvider;
import com.umc.product.demoday.application.port.in.command.CastDemodayVoteUseCase;
import com.umc.product.demoday.application.port.in.command.CollectDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayVoteAuthorizationUseCase;
import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteAuthorizationInfo;
import com.umc.product.demoday.application.port.in.command.dto.DemodayVoteInfo;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Demoday Vote | Participation", description = "회원과 외부 방문자의 데모데이 참여 명령 API")
@RestController
@RequestMapping("/api/v1/demoday/polls")
@RequiredArgsConstructor
public class DemodayParticipationCommandController {

    private final StartDemodayGuestParticipationUseCase startDemodayGuestParticipationUseCase;
    private final CollectDemodayStampUseCase collectDemodayStampUseCase;
    private final CreateDemodayVoteAuthorizationUseCase createDemodayVoteAuthorizationUseCase;
    private final CastDemodayVoteUseCase castDemodayVoteUseCase;
    private final DemodayParticipantTokenProvider demodayParticipantTokenProvider;
    private final DemodayParticipantCookieWriter demodayParticipantCookieWriter;

    @Operation(
        operationId = "startGuestParticipation",
        summary = "입장 코드 사용 및 외부 방문자 참여 시작",
        description = """
            외부 방문자가 현장에서 받은 입장 코드를 제출해 참여를 시작합니다.

            성공하면 서버가 코드를 사용 처리하고 요청한 브라우저에 1회 바인딩한 뒤,
            데모데이 전용 participant token을 HttpOnly Cookie로 설정합니다.
            같은 코드와 requestId로 재시도하면 Cookie를 다시 발급합니다. 다른 requestId로 이미 사용된
            코드를 제출하면 거절합니다. requestId를 생략한 기존 클라이언트는 기존 Cookie 기반 동작을 유지합니다.
            Cookie와 입장 코드의 수명은 Poll 종료 시점까지입니다.
            """
    )
    @Public
    @PostMapping("/{pollId}/participations/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayParticipationResponse startGuestParticipation(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Valid @RequestBody GuestParticipationRequest request,
        @Parameter(hidden = true)
        @CookieValue(name = DemodayParticipantTokenProvider.COOKIE_NAME, required = false) String existingToken,
        HttpServletResponse response
    ) {
        Long existingEntryCodeId = demodayParticipantTokenProvider.parseEntryCodeId(existingToken).orElse(null);

        StartDemodayGuestParticipationInfo info = startDemodayGuestParticipationUseCase.start(
            request.toCommand(pollId, existingEntryCodeId));

        demodayParticipantCookieWriter.writeParticipantCookie(response, info.participantToken(), info.expiresAt());

        return DemodayParticipationResponse.from(info.participation());
    }

    @Operation(
        operationId = "collectStamp",
        summary = "부스 QR을 통한 스탬프 적립",
        description = """
            부스에 비치된 QR을 스캔해 스탬프를 적립합니다.

            프로젝트 부스와 스탬프 적립 전용 외부 부스 모두 스탬프를 적립할 수 있습니다.
            서로 다른 부스의 스탬프를 최대 6개까지 적립하며, 새로 적립된 스탬프 사이에는 서버 시각 기준
            5분의 쿨다운이 있습니다. 같은 부스를 다시 스캔해도 새 스탬프를 만들지 않고 항상 현재 상태와
            함께 성공(201)으로 응답합니다. 재스캔은 쿨다운을 새로 시작하지 않습니다.
            """
    )
    @PostMapping("/{pollId}/stamps")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayStampCollectResponse collectStamp(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Valid @RequestBody StampCollectRequest request,
        @Parameter(hidden = true) @CurrentDemodayParticipant DemodayParticipant participant
    ) {
        DemodayStampCollectInfo info = collectDemodayStampUseCase.collect(request.toCommand(pollId, participant));

        return DemodayStampCollectResponse.from(info);
    }

    @Operation(
        operationId = "createVoteAuthorization",
        summary = "INFO QR 재인증 및 5분 투표 권한 발급",
        description = """
            사용자가 투표할 프로젝트 부스를 선택한 뒤 INFO 부스 QR을 스캔해 최종 투표 권한을 발급받습니다.
            외부 부스는 스탬프 적립 전용이므로 투표 권한을 발급하지 않습니다.
            INFO QR의 서명·용도·Poll과 참여자의 스탬프 6개·미투표 상태를 검증합니다.
            성공하면 선택 부스와 참여자에 결합된 별도 서명 token을 반환하며, 발급 시점부터 정확히 5분간 유효합니다.
            투표 전에는 다시 발급받을 수 있고, 최종적으로 먼저 저장된 한 표만 인정합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "투표 권한 발급 성공"),
        @ApiResponse(responseCode = "400", description = "요청 필드 형식 오류"),
        @ApiResponse(responseCode = "401", description = "참여자 인증 또는 INFO QR이 유효하지 않거나 만료됨"),
        @ApiResponse(responseCode = "403", description = "스탬프가 6개 미만이거나 소속 부스를 선택함"),
        @ApiResponse(responseCode = "404", description = "Poll 또는 선택 부스를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "투표 시간 아님, 이미 투표함, Poll과 부스 불일치, 외부 부스 선택(DEMODAY-0416)")
    })
    @PostMapping("/{pollId}/vote-authorizations")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayVoteAuthorizationResponse createVoteAuthorization(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Valid @RequestBody CreateDemodayVoteAuthorizationRequest request,
        @Parameter(hidden = true) @CurrentDemodayParticipant DemodayParticipant participant
    ) {
        DemodayVoteAuthorizationInfo info = createDemodayVoteAuthorizationUseCase.create(
            request.toCommand(pollId, participant));

        return DemodayVoteAuthorizationResponse.from(info);
    }

    @Operation(
        operationId = "castVote",
        summary = "선택한 부스에 최종 투표",
        description = """
            투표 권한 token에 결합된 프로젝트 부스에 표를 최종 저장합니다. boothId를 다시 받지 않습니다.
            외부 부스에 결합된 권한은 최종 저장 단계에서도 거절합니다.
            token의 서명·용도·만료·Poll·참여자 일치와 최종 저장 시점의 Poll 운영 시간을 검증합니다.
            같은 참여자의 여러 권한이 제출되면 DB 유일 제약으로 가장 먼저 저장된 한 표만 성공합니다.
            Poll이 종료되면 아직 token의 5분 유효 시간이 남아 있어도 제출을 거절합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "최종 투표 성공"),
        @ApiResponse(responseCode = "400", description = "요청 필드 형식 오류"),
        @ApiResponse(responseCode = "401", description = "투표 권한이 유효하지 않거나 만료됨"),
        @ApiResponse(responseCode = "403", description = "다른 참여자의 권한이거나 소속 부스에 투표함"),
        @ApiResponse(responseCode = "404", description = "Poll, token에 결합된 부스 또는 게스트 입장 코드를 찾을 수 없음"),
        @ApiResponse(responseCode = "409", description = "투표 시간 아님, 이미 투표함, Poll 불일치, 외부 부스 선택(DEMODAY-0416)")
    })
    @PostMapping("/{pollId}/votes")
    @ResponseStatus(HttpStatus.CREATED)
    public DemodayVoteResponse castVote(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Valid @RequestBody CastDemodayVoteRequest request,
        @Parameter(hidden = true) @CurrentDemodayParticipant DemodayParticipant participant
    ) {
        DemodayVoteInfo info = castDemodayVoteUseCase.cast(request.toCommand(pollId, participant));

        return DemodayVoteResponse.from(info);
    }
}
