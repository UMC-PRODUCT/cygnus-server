package com.umc.product.chat.application.service.query;

import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.GetMyChatRoomsUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.application.policy.ChatRoomAccessPolicy;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService implements GetChatMessagesUseCase, GetMyChatRoomsUseCase {

    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatRoomPort loadChatRoomPort;
    private final ChatRoomAccessPolicy chatRoomAccessPolicy;

    /**
     * 방 메시지 내역을 최신순 커서 페이지네이션으로 조회한다. (size + 1 조회 후 hasNext 판별)
     * <p>
     * 조회 전 요청자가 해당 방의 멤버인지 검증한다(비멤버는 접근 불가).
     */
    @Override
    public ChatMessageCursorResult getMessages(GetChatMessagesQuery query) {
        chatRoomAccessPolicy.verifyMember(query.roomId(), query.memberId());

        List<ChatMessage> rows = loadChatMessagePort.listByRoomId(query.roomId(), query.cursorId(), query.size() + 1);

        boolean hasNext = rows.size() > query.size();
        List<ChatMessage> page = hasNext ? rows.subList(0, query.size()) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;

        List<ChatMessageInfo> content = page.stream()
            .map(ChatMessageInfo::from)
            .toList();

        return new ChatMessageCursorResult(content, nextCursor, hasNext);
    }

    /**
     * 내가 속한 채팅방 목록을 마지막 메시지 미리보기 + 안 읽은 수와 함께 조회한다.
     * <p>
     * 방 개수와 무관하게 쿼리 3회로 고정한다(N+1 방지): ① 내 멤버십, ② 방별 마지막 메시지(배치),
     * ③ 방별 안 읽은 수(배치). 이후 메모리에서 조립한다.
     */
    @Override
    public List<ChatRoomSummaryInfo> getMyChatRooms(Long memberId) {
        List<Long> roomIds = loadChatRoomPort.listByMemberId(memberId).stream()
            .map(ChatRoom::getId)
            .toList();

        if (roomIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ChatMessage> lastByRoom = loadChatMessagePort.listLatestPerRoom(roomIds).stream()
            .collect(Collectors.toMap(ChatMessage::getRoomId, Function.identity()));

        Map<Long, Long> unreadByRoom = loadChatMessagePort.countUnreadByRooms(memberId, roomIds).stream()
            .collect(Collectors.toMap(RoomUnreadCount::roomId, RoomUnreadCount::unreadCount));

        return roomIds.stream()
            .map(roomId -> {
                ChatMessage last = lastByRoom.get(roomId);
                ChatMessageInfo lastInfo = last != null ? ChatMessageInfo.from(last) : null;
                long unread = unreadByRoom.getOrDefault(roomId, 0L);
                return new ChatRoomSummaryInfo(roomId, lastInfo, unread);
            })
            // 마지막 메시지 최신순(메시지 없는 방은 뒤로)
            .sorted(Comparator.comparingLong(
                (ChatRoomSummaryInfo s) -> s.lastMessage() != null ? s.lastMessage().messageId() : 0L).reversed())
            .toList();
    }
}
