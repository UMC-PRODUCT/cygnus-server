package com.umc.product.chat.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.ManageChatRoomPinnedMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.PinChatRoomMessageCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatRoomPort;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.event.ChatRoomPinnedMessageChangedEvent;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomCommandService implements
    CreateChatRoomUseCase,
    DeleteChatRoomUseCase,
    ManageChatRoomPinnedMessageUseCase {

    private final SaveChatRoomPort saveChatRoomPort;
    private final LoadChatRoomPort loadChatRoomPort;
    private final LoadChatMessagePort loadChatMessagePort;
    private final SaveChatMemberPort saveChatMemberPort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ChatRoomInfo create(CreateChatRoomCommand command) {
        ChatRoom chatRoom = saveChatRoomPort.save(ChatRoom.create());
        saveChatMemberPort.save(ChatMember.of(chatRoom.getId(), command.creatorMemberId()));
        return new ChatRoomInfo(
            chatRoom.getId(),
            chatRoom.getCreatedAt(),
            null,
            List.of(command.creatorMemberId())
        );
    }

    @Override
    public void delete(Long roomId) {
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        saveChatRoomPort.delete(chatRoom);
    }

    @Override
    public void pin(PinChatRoomMessageCommand command) {
        ChatRoom chatRoom = loadChatRoomPort.getById(command.roomId());
        loadChatMessagePort.getByIdAndRoomId(command.messageId(), command.roomId());
        chatRoom.pinMessage(command.messageId());
        saveChatRoomPort.save(chatRoom);
        domainEventPublisher.publish(ChatRoomPinnedMessageChangedEvent.of(command.roomId(), command.messageId()));
    }

    @Override
    public void unpin(Long roomId) {
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        chatRoom.unpinMessage();
        saveChatRoomPort.save(chatRoom);
        domainEventPublisher.publish(ChatRoomPinnedMessageChangedEvent.of(roomId, null));
    }
}
