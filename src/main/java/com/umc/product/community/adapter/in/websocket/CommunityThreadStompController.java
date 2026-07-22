package com.umc.product.community.adapter.in.websocket;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.umc.product.community.adapter.in.websocket.dto.request.ChangeCommunityThreadReactionRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.CreateCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.DeleteCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.EditCommunityThreadMessageRequest;
import com.umc.product.community.adapter.in.websocket.dto.request.UpdateCommunityThreadReadRequest;
import com.umc.product.community.application.port.in.command.thread.message.CreateCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.EditCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.ManageCommunityThreadMessageReactionUseCase;
import com.umc.product.community.application.port.in.command.thread.message.TombstoneCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.UpdateCommunityThreadReadUseCase;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReadMutationInfo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommunityThreadStompController {

    private final CreateCommunityThreadMessageUseCase createMessageUseCase;
    private final EditCommunityThreadMessageUseCase editMessageUseCase;
    private final TombstoneCommunityThreadMessageUseCase tombstoneMessageUseCase;
    private final ManageCommunityThreadMessageReactionUseCase manageReactionUseCase;
    private final UpdateCommunityThreadReadUseCase updateReadUseCase;
    private final CommunityStompCommandSupport commandSupport;

    @MessageMapping("/community/threads/{threadId}/messages")
    public void createMessage(
        @DestinationVariable Long threadId,
        @Valid @Payload CreateCommunityThreadMessageRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        UUID clientMessageId = request.clientMessageUuid();
        try {
            CommunityThreadMessageMutationInfo result = createMessageUseCase.create(
                request.toCommand(threadId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.MESSAGE_CREATE,
                    result.message().messageId(),
                    clientMessageId,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.MESSAGE_CREATE, clientMessageId),
                exception
            );
        }
    }

    @MessageMapping("/community/threads/{threadId}/messages/{messageId}/edit")
    public void editMessage(
        @DestinationVariable Long threadId,
        @DestinationVariable Long messageId,
        @Valid @Payload EditCommunityThreadMessageRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        try {
            CommunityThreadMessageMutationInfo result = editMessageUseCase.edit(
                request.toCommand(threadId, messageId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.MESSAGE_EDIT,
                    result.message().messageId(),
                    null,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.MESSAGE_EDIT, null),
                exception
            );
        }
    }

    @MessageMapping("/community/threads/{threadId}/messages/{messageId}/delete")
    public void deleteMessage(
        @DestinationVariable Long threadId,
        @DestinationVariable Long messageId,
        @Valid @Payload DeleteCommunityThreadMessageRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        try {
            CommunityThreadMessageMutationInfo result = tombstoneMessageUseCase.tombstone(
                request.toCommand(threadId, messageId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.MESSAGE_DELETE,
                    result.message().messageId(),
                    null,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.MESSAGE_DELETE, null),
                exception
            );
        }
    }

    @MessageMapping("/community/threads/{threadId}/messages/{messageId}/reactions/add")
    public void addReaction(
        @DestinationVariable Long threadId,
        @DestinationVariable Long messageId,
        @Valid @Payload ChangeCommunityThreadReactionRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        try {
            CommunityThreadReactionMutationInfo result = manageReactionUseCase.add(
                request.toCommand(threadId, messageId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.REACTION_ADD,
                    result.messageId(),
                    null,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.REACTION_ADD, null),
                exception
            );
        }
    }

    @MessageMapping("/community/threads/{threadId}/messages/{messageId}/reactions/remove")
    public void removeReaction(
        @DestinationVariable Long threadId,
        @DestinationVariable Long messageId,
        @Valid @Payload ChangeCommunityThreadReactionRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        try {
            CommunityThreadReactionMutationInfo result = manageReactionUseCase.remove(
                request.toCommand(threadId, messageId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.REACTION_REMOVE,
                    result.messageId(),
                    null,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.REACTION_REMOVE, null),
                exception
            );
        }
    }

    @MessageMapping("/community/threads/{threadId}/read")
    public void updateRead(
        @DestinationVariable Long threadId,
        @Valid @Payload UpdateCommunityThreadReadRequest request,
        Principal principal,
        Message<?> inboundMessage
    ) {
        CommunityStompCommandContext context = commandSupport.context(principal, inboundMessage);
        try {
            CommunityThreadReadMutationInfo result = updateReadUseCase.update(
                request.toCommand(threadId, context.memberId())
            );
            commandSupport.acknowledge(
                threadId,
                context,
                new CommunityStompCommandOutcome(
                    CommunityStompCommandType.READ_UPDATE,
                    result.lastReadMessageId(),
                    null,
                    result.deduplicated()
                )
            );
        } catch (RuntimeException exception) {
            commandSupport.reject(
                context.correlation(CommunityStompCommandType.READ_UPDATE, null),
                exception
            );
        }
    }

    @MessageExceptionHandler(Exception.class)
    public void handleMessageException(
        Exception exception,
        Principal principal,
        Message<?> inboundMessage
    ) {
        commandSupport.handleMessageException(exception, principal, inboundMessage);
    }
}
