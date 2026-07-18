package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadInvitationInfo;
import com.umc.product.community.application.port.in.command.thread.dto.InviteCommunityThreadMembersCommand;

public interface InviteCommunityThreadMembersUseCase {

    CommunityThreadInvitationInfo invite(InviteCommunityThreadMembersCommand command);
}
