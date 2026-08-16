package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.SearchThreadInvitableQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadInvitablePageInfo;

public interface SearchCommunityThreadInvitableUseCase {

    ThreadInvitablePageInfo searchInvitable(SearchThreadInvitableQuery query);
}
