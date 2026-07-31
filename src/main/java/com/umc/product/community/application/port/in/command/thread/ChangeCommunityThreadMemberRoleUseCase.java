package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.ChangeCommunityThreadMemberRoleCommand;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;

public interface ChangeCommunityThreadMemberRoleUseCase {

    CommunityThreadMemberLifecycleInfo changeRole(ChangeCommunityThreadMemberRoleCommand command);
}
