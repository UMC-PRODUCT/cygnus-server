package com.umc.product.community.application.port.in.query.thread.message;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessagePageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecoveryQuery;

public interface RecoverCommunityThreadMessagesUseCase {

    CommunityThreadMessagePageInfo recover(CommunityThreadMessageRecoveryQuery query);
}
