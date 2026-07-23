package com.umc.product.community.application.port.in.command.thread.message;

import com.umc.product.community.application.port.in.command.thread.message.dto.EditCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;

public interface EditCommunityThreadMessageUseCase {

    CommunityThreadMessageMutationInfo edit(EditCommunityThreadMessageCommand command);
}
