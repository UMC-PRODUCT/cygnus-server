package com.umc.product.community.adapter.in.web;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.umc.product.community.adapter.in.web.dto.request.ChangeCommunityThreadMemberRoleRequest;
import com.umc.product.community.adapter.in.web.dto.request.InviteCommunityThreadMembersRequest;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadInvitationResponse;
import com.umc.product.community.adapter.in.web.dto.response.CommunityThreadMemberMutationResponse;
import com.umc.product.community.adapter.in.web.validation.PositiveDecimalId;
import com.umc.product.community.application.port.in.command.thread.ChangeCommunityThreadMemberRoleUseCase;
import com.umc.product.community.application.port.in.command.thread.InviteCommunityThreadMembersUseCase;
import com.umc.product.community.application.port.in.command.thread.KickCommunityThreadMemberUseCase;
import com.umc.product.community.application.port.in.command.thread.LeaveCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadInvitationInfo;
import com.umc.product.community.application.port.in.command.thread.dto.KickCommunityThreadMemberCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.in.query.thread.GetCommunityThreadMembersByIdsUseCase;
import com.umc.product.community.application.port.in.query.thread.dto.GetThreadMembersByIdsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;
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
@Tag(name = "Community | Thread Membership", description = "Community thread 초대와 member lifecycle API")
public class CommunityThreadMembershipController {

    private final InviteCommunityThreadMembersUseCase inviteMembersUseCase;
    private final GetCommunityThreadMembersByIdsUseCase getMembersByIdsUseCase;
    private final KickCommunityThreadMemberUseCase kickMemberUseCase;
    private final LeaveCommunityThreadUseCase leaveThreadUseCase;
    private final ChangeCommunityThreadMemberRoleUseCase changeMemberRoleUseCase;

    @PostMapping("/threads/{threadId}/invite")
    @Operation(operationId = "COMMUNITY-THREAD-008", summary = "Community thread member 초대")
    public CommunityThreadInvitationResponse inviteMembers(
        @PathVariable @PositiveDecimalId String threadId,
        @Valid @RequestBody InviteCommunityThreadMembersRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        Long parsedThreadId = CommunityWebNumbers.id(threadId);
        CommunityThreadInvitationInfo invitation = inviteMembersUseCase.invite(
            request.toCommand(parsedThreadId, principal.getMemberId())
        );
        List<Long> invitedMemberIds = invitation.invitedMembers().stream()
            .map(member -> member.memberId())
            .toList();
        List<ThreadMemberInfo> invitedMembers = getMembersByIdsUseCase.getMembersByIds(
            new GetThreadMembersByIdsQuery(
                parsedThreadId,
                principal.getMemberId(),
                invitedMemberIds
            )
        );
        return CommunityThreadInvitationResponse.of(invitedMembers, invitation.memberCount());
    }

    @DeleteMapping("/threads/{threadId}/members/{memberId}")
    @Operation(operationId = "COMMUNITY-THREAD-009", summary = "Community thread member kick")
    public CommunityThreadMemberMutationResponse kickMember(
        @PathVariable @PositiveDecimalId String threadId,
        @PathVariable @PositiveDecimalId String memberId,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMemberMutationResponse.from(kickMemberUseCase.kick(
            new KickCommunityThreadMemberCommand(
                CommunityWebNumbers.id(threadId),
                principal.getMemberId(),
                CommunityWebNumbers.id(memberId)
            )
        ));
    }

    @PostMapping("/threads/{threadId}/leave")
    @Operation(operationId = "COMMUNITY-THREAD-010", summary = "Community thread leave")
    public CommunityThreadMemberMutationResponse leaveThread(
        @PathVariable @PositiveDecimalId String threadId,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMemberMutationResponse.from(leaveThreadUseCase.leave(
            new ThreadActorCommand(CommunityWebNumbers.id(threadId), principal.getMemberId())
        ));
    }

    @PatchMapping("/threads/{threadId}/members/{memberId}/role")
    @Operation(operationId = "COMMUNITY-THREAD-011", summary = "Community thread member role 변경")
    public CommunityThreadMemberMutationResponse changeRole(
        @PathVariable @PositiveDecimalId String threadId,
        @PathVariable @PositiveDecimalId String memberId,
        @Valid @RequestBody ChangeCommunityThreadMemberRoleRequest request,
        @CurrentMember MemberPrincipal principal
    ) {
        return CommunityThreadMemberMutationResponse.from(changeMemberRoleUseCase.changeRole(
            request.toCommand(
                CommunityWebNumbers.id(threadId),
                principal.getMemberId(),
                CommunityWebNumbers.id(memberId)
            )
        ));
    }
}
