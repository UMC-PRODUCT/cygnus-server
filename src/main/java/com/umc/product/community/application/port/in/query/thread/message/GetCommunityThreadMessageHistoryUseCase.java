package com.umc.product.community.application.port.in.query.thread.message;

import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageHistoryQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessagePageInfo;

public interface GetCommunityThreadMessageHistoryUseCase {

    CommunityThreadMessagePageInfo getHistory(CommunityThreadMessageHistoryQuery query);
}
