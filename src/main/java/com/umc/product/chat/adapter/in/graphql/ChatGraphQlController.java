package com.umc.product.chat.adapter.in.graphql;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import com.umc.product.chat.adapter.in.graphql.dto.ChatMessageGraphQlResponse;
import com.umc.product.chat.adapter.in.graphql.dto.ChatRoomGraphQlResponse;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.GetChatRoomUseCase;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessagesQuery;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.global.security.annotation.CurrentMember;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberPublicInfo;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatGraphQlController {

    private static final int DEFAULT_SIZE = 30;

    private final ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;
    private final GetChatRoomUseCase getChatRoomUseCase;
    private final GetChatMessagesUseCase getChatMessagesUseCase;
    private final GetMemberUseCase getMemberUseCase;

    @QueryMapping
    public List<ChatRoomGraphQlResponse.Summary> chatRooms(
        @CurrentMember MemberPrincipal principal,
        @Argument List<Long> ids
    ) {
        return listChatRoomSummariesUseCase.listRoomSummaries(principal.getMemberId(), ids).stream()
            .map(ChatRoomGraphQlResponse.Summary::from)
            .toList();
    }

    @QueryMapping
    public ChatRoomGraphQlResponse.Room chatRoom(
        @CurrentMember MemberPrincipal principal,
        @Argument Long id
    ) {
        return ChatRoomGraphQlResponse.Room.from(getChatRoomUseCase.getById(id, principal.getMemberId()));
    }

    @SchemaMapping(typeName = "ChatRoomSummary", field = "room")
    public ChatRoomGraphQlResponse.Room summaryRoom(
        ChatRoomGraphQlResponse.Summary summary,
        @CurrentMember MemberPrincipal principal
    ) {
        return ChatRoomGraphQlResponse.Room.from(
            getChatRoomUseCase.getById(summary.roomId(), principal.getMemberId())
        );
    }

    @SchemaMapping(typeName = "ChatRoom", field = "messages")
    public ChatRoomGraphQlResponse.Messages messages(
        ChatRoomGraphQlResponse.Room room,
        @CurrentMember MemberPrincipal principal,
        @Argument Long after,
        @Argument Integer first
    ) {
        int size = first == null ? DEFAULT_SIZE : first;
        var result = getChatMessagesUseCase.getMessages(
            new GetChatMessagesQuery(room.roomId(), principal.getMemberId(), after, size)
        );
        return new ChatRoomGraphQlResponse.Messages(
            result.content().stream().map(ChatMessageGraphQlResponse::from).toList(),
            result.nextCursor(),
            result.hasNext()
        );
    }

    @BatchMapping(typeName = "ChatRoom", field = "members")
    public Map<ChatRoomGraphQlResponse.Room, List<MemberPublicInfo>> members(
        List<ChatRoomGraphQlResponse.Room> rooms
    ) {
        Set<Long> memberIds = rooms.stream()
            .flatMap(room -> room.memberIds().stream())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<ChatRoomGraphQlResponse.Room, List<MemberPublicInfo>> result = new LinkedHashMap<>();
        for (ChatRoomGraphQlResponse.Room room : rooms) {
            result.put(
                room,
                room.memberIds().stream().map(membersById::get).filter(java.util.Objects::nonNull).toList()
            );
        }
        return result;
    }

    @BatchMapping(typeName = "ChatMessage", field = "sender")
    public Map<ChatMessageGraphQlResponse, MemberPublicInfo> senders(
        List<ChatMessageGraphQlResponse> messages
    ) {
        Set<Long> memberIds = messages.stream()
            .map(ChatMessageGraphQlResponse::senderMemberId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, MemberPublicInfo> membersById = getMemberUseCase.findAllByIds(memberIds).entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> MemberPublicInfo.from(entry.getValue())
            ));
        Map<ChatMessageGraphQlResponse, MemberPublicInfo> result = new LinkedHashMap<>();
        for (ChatMessageGraphQlResponse message : messages) {
            result.put(message, membersById.get(message.senderMemberId()));
        }
        return result;
    }
}
