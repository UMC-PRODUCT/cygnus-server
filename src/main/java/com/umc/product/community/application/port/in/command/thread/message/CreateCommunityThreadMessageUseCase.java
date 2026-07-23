package com.umc.product.community.application.port.in.command.thread.message;

import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;

public interface CreateCommunityThreadMessageUseCase {

    CommunityThreadMessageMutationInfo create(CreateCommunityThreadMessageCommand command);
}
