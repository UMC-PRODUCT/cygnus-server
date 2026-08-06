package com.umc.product.community.application.port.in.query.thread;

import java.util.List;

import com.umc.product.community.application.port.in.query.thread.dto.GetThreadMembersByIdsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadMemberInfo;

public interface GetCommunityThreadMembersByIdsUseCase {

    List<ThreadMemberInfo> getMembersByIds(GetThreadMembersByIdsQuery query);
}
