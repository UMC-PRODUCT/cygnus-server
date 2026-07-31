package com.umc.product.community.application.port.in.command.thread.message;

import com.umc.product.community.application.port.in.command.thread.message.dto.ChangeCommunityThreadMessageReactionCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionMutationInfo;

public interface ManageCommunityThreadMessageReactionUseCase {

    CommunityThreadReactionMutationInfo add(ChangeCommunityThreadMessageReactionCommand command);

    CommunityThreadReactionMutationInfo remove(ChangeCommunityThreadMessageReactionCommand command);
}
