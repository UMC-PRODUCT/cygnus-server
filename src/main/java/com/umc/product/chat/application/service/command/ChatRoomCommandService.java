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
import com.umc.product.chat.application.port.out.SaveChatRoomOwnershipPort;
import com.umc.product.chat.application.port.out.SaveChatRoomPort;
import com.umc.product.chat.application.service.ChatRoomOwnershipAccessService;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
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
    private final SaveChatRoomOwnershipPort saveChatRoomOwnershipPort;
    private final ChatRoomOwnershipAccessService ownershipAccessService;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public ChatRoomInfo create(CreateChatRoomCommand command) {
        ChatRoomActorContext actorContext = command.actorContext();
        ownershipAccessService.verifyAuthenticatedActor(actorContext);
        ChatRoom chatRoom = saveChatRoomPort.save(ChatRoom.create());
        ChatRoomOwnerReference ownerReference = ChatRoomOwnerReference.standalone(chatRoom.getId());
        saveChatRoomOwnershipPort.save(ownerReference);
        ownershipAccessService.verifyForUpdate(ownerReference, ChatRoomOperation.CREATE, actorContext);
        saveChatMemberPort.save(ChatMember.of(chatRoom.getId(), actorContext.actorMemberId()));
        return new ChatRoomInfo(
            chatRoom.getId(),
            chatRoom.getCreatedAt(),
            null,
            List.of(actorContext.actorMemberId())
        );
    }

    @Override
    public void delete(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext) {
        ownershipAccessService.verifyForUpdate(expectedOwner, ChatRoomOperation.DELETE, actorContext);
        Long roomId = expectedOwner.roomId();
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        saveChatRoomPort.delete(chatRoom);
    }

    @Override
    public void pin(PinChatRoomMessageCommand command) {
        ownershipAccessService.verifyForUpdate(
            command.expectedOwner(), ChatRoomOperation.PIN, command.actorContext());
        Long roomId = command.expectedOwner().roomId();
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        loadChatMessagePort.getByIdAndRoomId(command.messageId(), roomId);
        chatRoom.pinMessage(command.messageId());
        saveChatRoomPort.save(chatRoom);
        domainEventPublisher.publish(ChatRoomPinnedMessageChangedEvent.of(roomId, command.messageId()));
    }

    @Override
    public void unpin(ChatRoomOwnerReference expectedOwner, ChatRoomActorContext actorContext) {
        ownershipAccessService.verifyForUpdate(expectedOwner, ChatRoomOperation.UNPIN, actorContext);
        Long roomId = expectedOwner.roomId();
        ChatRoom chatRoom = loadChatRoomPort.getById(roomId);
        chatRoom.unpinMessage();
        saveChatRoomPort.save(chatRoom);
        domainEventPublisher.publish(ChatRoomPinnedMessageChangedEvent.of(roomId, null));
    }
}
