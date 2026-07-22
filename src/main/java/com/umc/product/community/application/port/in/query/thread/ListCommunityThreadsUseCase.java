package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;

public interface ListCommunityThreadsUseCase {

    ThreadListInfo listThreads(ListThreadsQuery query);
}
