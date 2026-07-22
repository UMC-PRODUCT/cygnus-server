package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;

public interface CreateCommunityThreadUseCase {

    CommunityThreadLifecycleInfo create(CreateCommunityThreadCommand command);
}
