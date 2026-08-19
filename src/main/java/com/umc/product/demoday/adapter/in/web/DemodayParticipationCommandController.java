package com.umc.product.demoday.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.request.GuestParticipationRequest;
import com.umc.product.demoday.adapter.in.web.dto.request.StampCollectRequest;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayParticipationResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayStampCollectResponse;
import com.umc.product.demoday.adapter.in.web.security.CurrentDemodayParticipant;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantCookieWriter;
import com.umc.product.demoday.adapter.in.web.security.DemodayParticipantTokenProvider;
import com.umc.product.demoday.application.port.in.command.CollectDemodayStampUseCase;
import com.umc.product.demoday.application.port.in.command.StartDemodayGuestParticipationUseCase;
import com.umc.product.demoday.application.port.in.command.dto.DemodayStampCollectInfo;
import com.umc.product.demoday.application.port.in.command.dto.StartDemodayGuestParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "데모데이 투표 - 참여자", description = "외부 방문자가 참여를 시작하는 API")
@RestController
@RequestMapping("/api/v1/demoday/polls")
@RequiredArgsConstructor
public class DemodayParticipationCommandController {

    private final StartDemodayGuestParticipationUseCase startDemodayGuestParticipationUseCase;
    private final CollectDemodayStampUseCase collectDemodayStampUseCase;
    private final DemodayParticipantTokenProvider demodayParticipantTokenProvider;
    private final DemodayParticipantCookieWriter demodayParticipantCookieWriter;

    @Operation(
        operationId = "startGuestParticipation",
        summary = "입장 코드 사용 및 외부 방문자 참여 시작",
        description = """
            외부 방문자가 현장에서 받은 입장 코드를 제출해 참여를 시작합니다.

            성공하면 서버가 코드를 사용 처리하고 요청한 브라우저에 1회 바인딩한 뒤,
            데모데이 전용 participant token을 HttpOnly Cookie로 설정합니다.
            이미 유효한 Cookie를 가진 같은 브라우저가 같은 코드를 다시 제출하면 성공으로 처리합니다.
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
}
