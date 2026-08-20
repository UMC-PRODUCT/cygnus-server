package com.umc.product.demoday.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.response.DemodayBoothListResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayParticipationResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayPollListResponse;
import com.umc.product.demoday.adapter.in.web.security.CurrentDemodayParticipant;
import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipant;
import com.umc.product.global.security.annotation.Public;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "데모데이 투표", description = "데모데이 투표 참여자가 사용하는 조회 API")
@RestController
@RequestMapping("/api/v1/demoday/polls")
@RequiredArgsConstructor
public class DemodayPollQueryController {

    private final ListDemodayPollUseCase listDemodayPollUseCase;
    private final GetDemodayParticipationUseCase getDemodayParticipationUseCase;
    private final ListDemodayBoothUseCase listDemodayBoothUseCase;

    @Operation(summary = "데모데이 투표 목록 조회", description = "데모데이 투표 목록을 조회합니다.")
    @Public
    @GetMapping
    public DemodayPollListResponse listPolls() {
        return DemodayPollListResponse.from(listDemodayPollUseCase.listPolls());
    }

    @Operation(
        summary = "내 투표 참여 정보 조회",
        description = "스탬프와 투표 기록을 기반으로 내 투표 참여 정보를 조회합니다. 회원 Bearer 또는 게스트 participant "
            + "Cookie 둘 중 하나만 있으면 통과합니다."
    )
    @GetMapping("/{pollId}/participations/me")
    public DemodayParticipationResponse getMyParticipation(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Parameter(hidden = true) @CurrentDemodayParticipant DemodayParticipant participant
    ) {
        DemodayParticipationInfo participation = getDemodayParticipationUseCase.getParticipation(pollId, participant);

        return DemodayParticipationResponse.from(participation);
    }

    @Operation(
        summary = "투표 대상 프로젝트 부스 목록 조회",
        description = "참여자가 투표할 수 있는 프로젝트 부스만 부스 코드 오름차순으로 조회합니다. 스탬프 적립 전용인 외부 부스는 제외됩니다."
    )
    @GetMapping("/{pollId}/booths")
    public DemodayBoothListResponse listBooths(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId
    ) {
        List<DemodayBoothInfo> boothInfos = listDemodayBoothUseCase.listBooths(pollId);
        return DemodayBoothListResponse.from(boothInfos);
    }
}
