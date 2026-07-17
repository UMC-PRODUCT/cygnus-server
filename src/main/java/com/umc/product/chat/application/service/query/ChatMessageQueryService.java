package com.umc.product.chat.application.service.query;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.query.CheckChatMessageReadUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageCursorResult;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReadStatusInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.application.port.in.query.dto.CheckChatMessageReadQuery;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageQueryService implements
    GetChatMessagesUseCase,
    ListChatRoomSummariesUseCase,
    CheckChatMessageReadUseCase {

    private final LoadChatMessagePort loadChatMessagePort;
    private final LoadChatMemberPort loadChatMemberPort;
    private final ChatRoomOwnershipAccessService ownershipAccessService;

    /**
     * 방 메시지 내역을 최신순 커서 페이지네이션으로 조회한다. (size + 1 조회 후 hasNext 판별)
     * <p>
     * 조회 전 요청자가 해당 방의 멤버인지 검증한다(비멤버는 접근 불가).
     */
    @Override
    public ChatMessageCursorResult getMessages(GetChatMessagesQuery query) {
        ownershipAccessService.verify(query.expectedOwner(), ChatRoomOperation.READ, query.actorContext());
        Long roomId = query.expectedOwner().roomId();

        List<ChatMessage> rows = loadChatMessagePort.listByRoomId(roomId, query.cursorId(), query.size() + 1);

        boolean hasNext = rows.size() > query.size();
        List<ChatMessage> page = hasNext ? rows.subList(0, query.size()) : rows;
        Long nextCursor = hasNext && !page.isEmpty() ? page.get(page.size() - 1).getId() : null;

        List<ChatMessageInfo> content = page.stream()
            .map(ChatMessageInfo::from)
            .toList();

        return new ChatMessageCursorResult(content, nextCursor, hasNext);
    }

    /**
     * 특정 방의 특정 메시지를 대상 멤버가 읽었는지 확인한다.
     * <p>
     * 요청자와 대상자 모두 방 멤버여야 하며, 메시지는 해당 방에 속해야 한다.
     */
    @Override
    public ChatMessageReadStatusInfo checkRead(CheckChatMessageReadQuery query) {
        ownershipAccessService.verify(query.expectedOwner(), ChatRoomOperation.READ, query.actorContext());
        Long roomId = query.expectedOwner().roomId();
        Long targetMemberId = query.actorContext().targetMemberId();

        ChatMessage message = loadChatMessagePort.getByIdAndRoomId(query.messageId(), roomId);
        ChatMember targetMember = loadChatMemberPort.getByRoomIdAndMemberId(roomId, targetMemberId);

        boolean read = isReadByTarget(message, targetMember);
        return new ChatMessageReadStatusInfo(roomId, query.messageId(), targetMemberId, read);
    }

    /**
     * 소비 도메인이 소유한 roomId 집합에 대해 방 요약(마지막 메시지 미리보기 + 안 읽은 수)을 조회한다.
     * <p>
     * 엔진은 멤버의 방을 스스로 열거하지 않는다. 전달받은 roomId 집합을 멤버가 실제 참여 중인 방으로 좁힌 뒤에만 조립하므로, 서로 다른 소비 도메인의 방이 한 응답에 섞이지 않는다(도메인 간 데이터
     * 격리).
     * <p>
     * ownership/membership 인가가 끝난 뒤 메시지 조립 쿼리는 방 개수와 무관하게 3회로 고정한다.
     */
    @Override
    public List<ChatRoomSummaryInfo> listRoomSummaries(
        ChatRoomActorContext actorContext,
        List<ChatRoomOwnerReference> expectedOwners
    ) {
        ownershipAccessService.verifyAuthenticatedActor(actorContext);
        if (expectedOwners == null || expectedOwners.isEmpty()) {
            return List.of();
        }

        expectedOwners.forEach(expectedOwner ->
            ownershipAccessService.verify(expectedOwner, ChatRoomOperation.READ, actorContext));
        List<Long> roomIds = expectedOwners.stream()
            .map(ChatRoomOwnerReference::roomId)
            .toList();
        Long memberId = actorContext.actorMemberId();

        // 소비 도메인이 넘긴 방 중 멤버가 실제 참여 중인 방으로 한정한다(격리 + 방어).
        List<Long> scopedRoomIds = loadChatMemberPort.listRoomIdsByMemberIdAndRoomIdIn(memberId, roomIds);
        if (scopedRoomIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ChatMessage> lastByRoom = loadChatMessagePort.listLatestPerRoom(scopedRoomIds).stream()
            .collect(Collectors.toMap(ChatMessage::getRoomId, Function.identity()));

        Map<Long, Long> unreadByRoom = loadChatMessagePort.countUnreadByRooms(memberId, scopedRoomIds).stream()
            .collect(Collectors.toMap(RoomUnreadCount::roomId, RoomUnreadCount::unreadCount));

        return scopedRoomIds.stream()
            .map(roomId -> {
                ChatMessage last = lastByRoom.get(roomId);
                ChatMessageInfo lastInfo = last != null ? ChatMessageInfo.from(last) : null;
                long unread = unreadByRoom.getOrDefault(roomId, 0L);
                return new ChatRoomSummaryInfo(roomId, lastInfo, unread);
            })
            .sorted(Comparator.comparingLong(
                (ChatRoomSummaryInfo s) -> s.lastMessage() != null ? s.lastMessage().messageId() : 0L).reversed())
            .toList();
    }

    private boolean isReadByTarget(ChatMessage message, ChatMember targetMember) {
        Long lastReadMessageId = targetMember.getLastReadMessageId();
        return lastReadMessageId != null && lastReadMessageId >= message.getId();
    }
}
