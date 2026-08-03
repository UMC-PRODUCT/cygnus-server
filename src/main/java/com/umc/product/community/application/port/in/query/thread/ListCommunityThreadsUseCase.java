package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.ListThreadsQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadListInfo;

/**
 * 현재 커뮤니티 탭에서 스레드 목록은 모든 스레드가 전체 공개로 조회되기 때문에, 이 UseCase는 어떤 엔드포인트에서도 호출되지 않는다. 신규 화면은
 * {@link BrowseCommunityThreadsUseCase}를 사용한다.
 *
 * <p>하지만 삭제하지 않고 남겨 둔다. requester가 ACTIVE 멤버인 스레드만 고정/일반으로 분리해 반환하는
 * 동작은 향후 "내 참여 스레드만 모아보기" 화면의 조회 기반으로 그대로 재사용할 가능성이 있기 때문이다.
 *
 * @deprecated 스레 목록은 {@link BrowseCommunityThreadsUseCase}를 사용한다. 본 UseCase는 참여 스레드 전용 조회 재사용을 위해 보류한 상태다.
 */
@Deprecated
public interface ListCommunityThreadsUseCase {

    ThreadListInfo listThreads(ListThreadsQuery query);
}
