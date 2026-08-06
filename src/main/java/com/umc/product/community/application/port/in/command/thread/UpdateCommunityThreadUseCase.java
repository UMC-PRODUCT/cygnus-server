package com.umc.product.community.application.port.in.command.thread;

import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;

public interface UpdateCommunityThreadUseCase {

    CommunityThreadLifecycleInfo update(UpdateCommunityThreadCommand command);
}
