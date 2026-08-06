package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.BrowseThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;

public interface BrowseCommunityThreadsUseCase {

    ThreadListInfo browseThreads(BrowseThreadsQuery query);
}
