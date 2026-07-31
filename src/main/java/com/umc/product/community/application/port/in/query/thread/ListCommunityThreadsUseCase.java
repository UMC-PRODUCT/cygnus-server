package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;

/**
 * @deprecated 전체 공개 통합 리스트로 전환되어 {@link BrowseCommunityThreadsUseCase}로 대체된다.
 *     초대받은 멤버의 스레드만 반환하므로 신규 화면에서는 사용하지 않는다.
 */
@Deprecated
public interface ListCommunityThreadsUseCase {

    ThreadListInfo listThreads(ListThreadsQuery query);
}
