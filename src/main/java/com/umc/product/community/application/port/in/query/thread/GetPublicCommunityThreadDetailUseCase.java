package com.umc.product.community.application.port.in.query.thread;

import com.umc.product.community.application.port.in.query.thread.dto.GetThreadDetailQuery;
import com.umc.product.community.application.port.in.query.thread.dto.ThreadDetailInfo;

/**
 * 스레드 상세를 <b>전체 공개</b>로 열람한다. 인증된 활성 회원이면 <b>이 스레드의 참여자가 아니어도</b> 조회할 수 있으며, 스레드 삭제 여부(soft delete)만 검증한다. 스레드 ACTIVE
 * 참여 여부는 검증하지 않고 응답의 {@code isJoined}로 requester의 참여 여부를 구분한다.
 *
 * <p>커뮤니티 탭에서 스레드를 둘러보는 조회 경로에 사용한다. 참여자 권한을 전제하는 경로
 * (STOMP 메시지 발송 인가 등)에는 사용하면 안 된다 -- 그 용도는 스레드 ACTIVE 참여자까지 검증하는 {@link GetJoinedCommunityThreadDetailUseCase#getJoinedThread}를
 * 사용한다.
 */
public interface GetPublicCommunityThreadDetailUseCase {

    ThreadDetailInfo getPublicThread(GetThreadDetailQuery query);
}
