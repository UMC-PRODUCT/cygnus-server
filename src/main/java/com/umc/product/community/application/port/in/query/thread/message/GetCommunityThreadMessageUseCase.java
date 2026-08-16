package com.umc.product.community.application.port.in.query.thread.message;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageQuery;

public interface GetCommunityThreadMessageUseCase {

    CommunityThreadMessageInfo getMessage(CommunityThreadMessageQuery query);
}
