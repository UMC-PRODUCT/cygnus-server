package com.umc.product.community.adapter.in.web.dto.request;

import java.util.List;

import org.hibernate.validator.constraints.UniqueElements;

import com.umc.product.community.application.port.in.command.thread.dto.InviteCommunityThreadMembersCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InviteCommunityThreadMembersRequest(
    @NotEmpty @Size(max = 99) @UniqueElements List<@NotNull @Positive Long> memberIds
) {

    public InviteCommunityThreadMembersRequest {
        memberIds = memberIds == null ? null : List.copyOf(memberIds);
    }

    public InviteCommunityThreadMembersCommand toCommand(Long threadId, Long actorMemberId) {
        return new InviteCommunityThreadMembersCommand(threadId, actorMemberId, memberIds);
    }
}
