package com.umc.product.chat.application.port.in.query;

import java.util.List;

import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;

/**
 * 소비 도메인이 소유한 채팅방 id 집합에 대해 방 요약(마지막 메시지 + 안 읽은 수)을 조회한다.
 * <p>
 * 엔진은 "이 멤버의 모든 방"을 스스로 열거하지 않는다. 소비 도메인이 자기 aggregate 에 보관한 engine roomId 집합을 넘기면, 그 집합 중 멤버가 실제 참여 중인 방만 요약해 돌려준다. 서로
 * 다른 소비 도메인 (inquiry / community 등)의 방이 한 응답에 섞이지 않도록 한다.
 *
 * @param memberId 조회 주체 멤버 id
 * @param roomIds  소비 도메인이 소유·전달한 engine roomId 집합. 이 집합 밖의 방은 결과에 포함되지 않는다.
 */
public interface ListChatRoomSummariesUseCase {

    List<ChatRoomSummaryInfo> listRoomSummaries(Long memberId, List<Long> roomIds);
}
