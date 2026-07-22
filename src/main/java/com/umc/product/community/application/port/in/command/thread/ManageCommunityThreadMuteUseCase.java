package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;

public interface ManageCommunityThreadMuteUseCase {

    CommunityThreadLifecycleInfo mute(ThreadActorCommand command);

    CommunityThreadLifecycleInfo unmute(ThreadActorCommand command);
}
