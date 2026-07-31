package com.umc.product.community.application.port.in.command.thread.message;

import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReadMutationInfo;

public interface UpdateCommunityThreadReadUseCase {

    CommunityThreadReadMutationInfo update(UpdateCommunityThreadReadCommand command);
}
