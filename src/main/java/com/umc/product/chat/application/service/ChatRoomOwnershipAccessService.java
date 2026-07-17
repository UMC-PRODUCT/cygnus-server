package com.umc.product.chat.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.policy.ChatRoomOwnerPolicyRegistry;
import com.umc.product.chat.application.port.out.ChatRoomOwnerPolicy;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.application.port.out.LoadChatRoomOwnershipPort;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOperation;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

import lombok.RequiredArgsConstructor;

/** Ownership binding, namespace policy, Chat membership을 순서대로 평가하는 단일 인가 진입점이다. */
@Service
@RequiredArgsConstructor
public class ChatRoomOwnershipAccessService {

    private final ChatRoomOwnerPolicyRegistry policyRegistry;
    private final LoadChatRoomOwnershipPort loadOwnershipPort;
    private final LoadChatMemberPort loadChatMemberPort;

    public void verify(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext
    ) {
        verifyAuthenticatedActor(actorContext);
        ChatRoomOwnerPolicy policy = policyRegistry.requirePolicy(expectedNamespace(expectedOwner));
        Optional<ChatRoomOwnerReference> persisted = loadOwnershipPort.findByRoomId(expectedOwner.roomId());
        verifyResolved(expectedOwner, operation, actorContext, policy, persisted);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyForUpdate(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext
    ) {
        verifyAuthenticatedActor(actorContext);
        ChatRoomOwnerPolicy policy = policyRegistry.requirePolicy(expectedNamespace(expectedOwner));
        Optional<ChatRoomOwnerReference> persisted =
            loadOwnershipPort.findByRoomIdForUpdate(expectedOwner.roomId());
        verifyResolved(expectedOwner, operation, actorContext, policy, persisted);
    }

    public boolean isAllowed(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext
    ) {
        try {
            verify(expectedOwner, operation, actorContext);
            return true;
        } catch (ChatDomainException exception) {
            return false;
        }
    }

    public void verifyAuthenticatedActor(ChatRoomActorContext actorContext) {
        if (actorContext == null || !actorContext.hasAuthenticatedActor()) {
            throw accessDenied();
        }
    }

    private void verifyResolved(
        ChatRoomOwnerReference expectedOwner,
        ChatRoomOperation operation,
        ChatRoomActorContext actorContext,
        ChatRoomOwnerPolicy policy,
        Optional<ChatRoomOwnerReference> persisted
    ) {
        ChatRoomOwnerReference actualOwner = persisted.orElseThrow(this::accessDenied);
        if (!actualOwner.sameBinding(expectedOwner)
            || operation == null
            || !policy.allows(actualOwner, operation, actorContext)) {
            throw accessDenied();
        }
        if (requiresMembership(operation)
            && !loadChatMemberPort.existsByRoomIdAndMemberId(actualOwner.roomId(), actorContext.actorMemberId())) {
            throw accessDenied();
        }
    }

    private String expectedNamespace(ChatRoomOwnerReference expectedOwner) {
        verifyExpectedOwner(expectedOwner);
        return expectedOwner.namespace();
    }

    private void verifyExpectedOwner(ChatRoomOwnerReference expectedOwner) {
        if (expectedOwner == null) {
            throw accessDenied();
        }
    }

    private boolean requiresMembership(ChatRoomOperation operation) {
        return operation == ChatRoomOperation.READ || operation == ChatRoomOperation.SEND;
    }

    private ChatDomainException accessDenied() {
        return new ChatDomainException(ChatErrorCode.CHAT_ROOM_ACCESS_DENIED);
    }
}
