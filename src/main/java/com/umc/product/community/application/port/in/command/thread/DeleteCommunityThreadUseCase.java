package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;

public interface DeleteCommunityThreadUseCase {

    CommunityThreadLifecycleInfo delete(ThreadActorCommand command);
}
