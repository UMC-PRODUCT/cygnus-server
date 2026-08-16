package com.umc.product.demoday.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.response.DemodayBoothListResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayParticipationResponse;
import com.umc.product.demoday.adapter.in.web.dto.response.DemodayPollListResponse;
import com.umc.product.demoday.application.port.in.query.GetDemodayParticipationUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayBoothUseCase;
import com.umc.product.demoday.application.port.in.query.ListDemodayPollUseCase;
import com.umc.product.demoday.application.port.in.query.dto.DemodayBoothInfo;
import com.umc.product.demoday.application.port.in.query.dto.DemodayParticipationInfo;
import com.umc.product.demoday.application.port.in.query.participant.DemodayParticipantResolver;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
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
    private final DemodayParticipantResolver<MemberPrincipal> participantResolver;

    @Operation(summary = "데모데이 투표 목록 조회", description = "데모데이 투표 목록을 조회합니다.")
    @Public
    @GetMapping
    public DemodayPollListResponse listPolls() {
        return DemodayPollListResponse.from(listDemodayPollUseCase.listPolls());
    }

    @Operation(summary = "내 투표 참여 정보 조회", description = "스탬프와 투표 기록을 기반으로 내 투표 참여 정보를 조회합니다.")
    @GetMapping("/{pollId}/participations/me")
    public DemodayParticipationResponse getMyParticipation(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId,
        @Parameter(hidden = true) @CurrentMember MemberPrincipal memberPrincipal
    ) {
        DemodayParticipationInfo participation = getDemodayParticipationUseCase.getParticipation(
                pollId, participantResolver.resolve(memberPrincipal));

        return DemodayParticipationResponse.from(participation);
    }

    @Operation(summary = "투표 부스 목록 조회", description = "투표에 등록된 부스 목록을 조회합니다.")
    @GetMapping("/{pollId}/booths")
    public DemodayBoothListResponse listBooths(
        @Parameter(description = "투표 ID", example = "1") @PathVariable Long pollId
    ) {
        List<DemodayBoothInfo> boothInfos = listDemodayBoothUseCase.listBooths(pollId);
        return DemodayBoothListResponse.from(boothInfos);
    }
}
