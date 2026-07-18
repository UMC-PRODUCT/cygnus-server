package com.umc.product.community.application.port.in.command.thread.message;

import com.umc.product.community.application.port.in.command.thread.message.dto.TombstoneCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;

public interface TombstoneCommunityThreadMessageUseCase {

    CommunityThreadMessageMutationInfo tombstone(TombstoneCommunityThreadMessageCommand command);
}
