package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;

/**
 * 스레드 상세를 <b>이 스레드의 ACTIVE 참여자에게만</b> 열람시킨다. 스레드 삭제 여부와 함께 requester가 해당 스레드의 ACTIVE 멤버인지 검증하며, 참여자가 아니면
 * {@code THREAD_ACCESS_DENIED}로 거부한다. (인증된 활성 회원 여부는 상위 보안 계층에서 이미 강제된다.)
 *
 * <p>참여자만 접근 가능해야 하는 경로 -- STOMP 메시지 발송 인가, 실시간 스냅샷 등 -- 의 게이트로 사용한다.
 * 참여 여부와 무관하게 열람만 허용하는 공개 조회에는 {@link GetPublicCommunityThreadDetailUseCase#getPublicThread}를 사용한다.
 */
public interface GetJoinedCommunityThreadDetailUseCase {

    ThreadDetailInfo getJoinedThread(GetThreadDetailQuery query);
}
