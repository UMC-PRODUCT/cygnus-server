package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.ListThreadMembersQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberPageInfo;

public interface ListCommunityThreadMembersUseCase {

    ThreadMemberPageInfo listMembers(ListThreadMembersQuery query);
}
