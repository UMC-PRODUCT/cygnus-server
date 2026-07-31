package com.umc.product.community.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.community.adapter.in.web.dto.request.CreateCommunityThreadRequest;
import com.umc.product.community.adapter.in.web.dto.request.UpdateCommunityThreadRequest;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadDetailResponse;
import com.umc.product.community.adapter.in.web.validation.PositiveDecimalId;
import com.umc.product.community.application.port.in.command.thread.CreateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.DeleteCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadMuteUseCase;
import com.umc.product.community.application.port.in.command.thread.ManageCommunityThreadPinUseCase;
import com.umc.product.community.application.port.in.command.thread.UpdateCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMutationDetailUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
@Validated
@Tag(name = "Community | Thread Lifecycle", description = "Community thread 생성, 수정, 삭제와 설정 API")
public class CommunityThreadLifecycleController {

    private final CreateCommunityThreadUseCase createThreadUseCase;
    private final UpdateCommunityThreadUseCase updateThreadUseCase;
    private final DeleteCommunityThreadUseCase deleteThreadUseCase;
    private final ManageCommunityThreadPinUseCase managePinUseCase;
    private final ManageCommunityThreadMuteUseCase manageMuteUseCase;
    private final GetCommunityThreadDetailUseCase getThreadDetailUseCase;
    private final GetCommunityThreadMutationDetailUseCase getMutationDetailUseCase;

    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "COMMUNITY-THREAD-001", summary = "Community thread 생성")
    public CommunityThreadDetailResponse createThread(
        @Valid @RequestBody CreateCommunityThreadRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        CommunityThreadLifecycleInfo created = createThreadUseCase.create(
            request.toCommand(principal.getMemberId())
        );
        return detail(created.threadId(), principal.getMemberId());
    }

    @PatchMapping("/threads/{threadId}")
    @Operation(operationId = "COMMUNITY-THREAD-002", summary = "Community thread metadata 수정")
    public CommunityThreadDetailResponse updateThread(
        @PathVariable @PositiveDecimalId String threadId,
        @Valid @RequestBody UpdateCommunityThreadRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        Long parsedThreadId = CommunityWebNumbers.id(threadId);
        updateThreadUseCase.update(request.toCommand(parsedThreadId, principal.getMemberId()));
        return detail(parsedThreadId, principal.getMemberId());
    }

    @DeleteMapping("/threads/{threadId}")
    @Operation(operationId = "COMMUNITY-THREAD-003", summary = "Community thread soft delete")
    public CommunityThreadDetailResponse deleteThread(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        Long parsedThreadId = CommunityWebNumbers.id(threadId);
        deleteThreadUseCase.delete(new ThreadActorCommand(parsedThreadId, principal.getMemberId()));
        return CommunityThreadDetailResponse.from(getMutationDetailUseCase.getMutationDetail(
            new GetThreadDetailQuery(parsedThreadId, principal.getMemberId())
        ));
    }

    @PostMapping("/threads/{threadId}/pin")
    @Operation(operationId = "COMMUNITY-THREAD-004", summary = "Community thread pin")
    public CommunityThreadDetailResponse pin(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return changeSetting(threadId, principal, SettingMutation.PIN);
    }

    @DeleteMapping("/threads/{threadId}/pin")
    @Operation(operationId = "COMMUNITY-THREAD-005", summary = "Community thread unpin")
    public CommunityThreadDetailResponse unpin(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return changeSetting(threadId, principal, SettingMutation.UNPIN);
    }

    @PostMapping("/threads/{threadId}/mute")
    @Operation(operationId = "COMMUNITY-THREAD-006", summary = "Community thread mute")
    public CommunityThreadDetailResponse mute(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return changeSetting(threadId, principal, SettingMutation.MUTE);
    }

    @DeleteMapping("/threads/{threadId}/mute")
    @Operation(operationId = "COMMUNITY-THREAD-007", summary = "Community thread unmute")
    public CommunityThreadDetailResponse unmute(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return changeSetting(threadId, principal, SettingMutation.UNMUTE);
    }

    private CommunityThreadDetailResponse changeSetting(
        String threadId,
        MemberPrincipal principal,
        SettingMutation mutation
    ) {
        Long parsedThreadId = CommunityWebNumbers.id(threadId);
        ThreadActorCommand command = new ThreadActorCommand(parsedThreadId, principal.getMemberId());
        switch (mutation) {
            case PIN -> managePinUseCase.pin(command);
            case UNPIN -> managePinUseCase.unpin(command);
            case MUTE -> manageMuteUseCase.mute(command);
            case UNMUTE -> manageMuteUseCase.unmute(command);
        }
        return detail(parsedThreadId, principal.getMemberId());
    }

    private CommunityThreadDetailResponse detail(Long threadId, Long memberId) {
        return CommunityThreadDetailResponse.from(getThreadDetailUseCase.getThread(
            new GetThreadDetailQuery(threadId, memberId)
        ));
    }

    private enum SettingMutation {
        PIN,
        UNPIN,
        MUTE,
        UNMUTE
    }
}
