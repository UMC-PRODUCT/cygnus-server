package com.umc.product.demoday.adapter.in.web;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.demoday.adapter.in.web.dto.ChangeDemodayPollStatusRequest;
import com.umc.product.demoday.adapter.in.web.dto.CreateDemodayPollRequest;
import com.umc.product.demoday.adapter.in.web.dto.CreateDemodayPollResponse;
import com.umc.product.demoday.application.port.in.command.ChangeDemodayPollStatusUseCase;
import com.umc.product.demoday.application.port.in.command.CreateDemodayPollUseCase;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/demoday/admin/polls")
@RequiredArgsConstructor
public class DemodayPollAdminController {

    private final CreateDemodayPollUseCase createDemodayPollUseCase;
    private final ChangeDemodayPollStatusUseCase changeDemodayPollStatusUseCase;

    @PostMapping
    public CreateDemodayPollResponse createPoll(
        @CurrentMember MemberPrincipal memberPrincipal,
        @Valid @RequestBody CreateDemodayPollRequest request) {

        Long pollId = createDemodayPollUseCase.create(request.toCommand(memberPrincipal.getMemberId()));

        return CreateDemodayPollResponse.from(pollId);
    }

    @PatchMapping("/{pollId}/status")
    public void changePollStatus(
        @CurrentMember MemberPrincipal memberPrincipal,
        @PathVariable("pollId") Long pollId,
        @Valid @RequestBody ChangeDemodayPollStatusRequest request) {

        changeDemodayPollStatusUseCase.changeStatus(request.toCommand(pollId, memberPrincipal.getMemberId()));
    }
}
