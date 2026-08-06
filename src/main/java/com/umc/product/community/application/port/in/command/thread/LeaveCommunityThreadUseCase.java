package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadMemberLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;

public interface LeaveCommunityThreadUseCase {

    CommunityThreadMemberLifecycleInfo leave(ThreadActorCommand command);
}
