package com.umc.product.chat.application.port.out;

import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMessage;
import java.util.List;
import java.util.Optional;

/**
 * 채팅 메시지 조회 포트.
 * <p>
 * 추후 이 포트를 구현하는 캐시 데코레이터 어댑터를 끼워 조회 성능 개선 예정
 */
public interface LoadChatMessagePort {

    ChatMessage getById(Long messageId);

    /**
     * 방 단위 메시지 내역을 최신순(id DESC)으로 커서 페이지네이션 조회한다.
     *
     * @param cursorId 이 id보다 작은(더 과거) 메시지를 조회. null이면 가장 최신부터.
     * @param size     조회 개수
     */
    List<ChatMessage> listByRoomId(Long roomId, Long cursorId, int size);

    /**
     * 여러 방의 마지막(가장 최신) 메시지를 한 번에 조회한다. (채팅방 목록 미리보기용)
     */
    List<ChatMessage> listLatestPerRoom(List<Long> roomIds);

    /**
     * 방의 가장 최신 메시지 id를 조회한다. 메시지가 없으면 {@code Optional.empty()}. (서버 주도 읽음 처리용)
     */
    Optional<Long> findLatestMessageId(Long roomId);

    /**
     * 멤버 기준으로 여러 방의 안 읽은 메시지 수를 한 번에 조회한다. 안 읽은 메시지가 0개인 방은 결과에 포함되지 않을 수 있다.
     */
    List<RoomUnreadCount> countUnreadByRooms(Long memberId, List<Long> roomIds);
}
