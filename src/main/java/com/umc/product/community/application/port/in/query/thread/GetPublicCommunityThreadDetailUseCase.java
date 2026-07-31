package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;

public interface GetPublicCommunityThreadDetailUseCase {

    ThreadDetailInfo getPublicThread(GetThreadDetailQuery query);
}
