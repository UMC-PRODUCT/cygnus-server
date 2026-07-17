package com.umc.product.chat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.umc.product.chat.application.port.out.LoadChatRoomOwnershipPort;
import com.umc.product.chat.application.port.out.SaveChatRoomOwnershipPort;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.ChatRoomOwnership;

import lombok.RequiredArgsConstructor;

/** PostgreSQL ownership registry adapter. */
@Component
@RequiredArgsConstructor
public class ChatRoomOwnershipPersistenceAdapter
    implements LoadChatRoomOwnershipPort, SaveChatRoomOwnershipPort {

    private final ChatRoomOwnershipJpaRepository repository;

    @Override
    @Transactional
    public ChatRoomOwnerReference save(ChatRoomOwnerReference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("chat room owner reference는 필수입니다.");
        }

        repository.insertIfAbsent(
            reference.roomId(),
            reference.namespace(),
            reference.ownerResourceKey(),
            reference.slot()
        );

        // INSERT가 경합으로 no-op이어도 방 row를 잠근 뒤 binding을 재확인해야
        // 같은 binding은 idempotent하게, 다른 binding은 transfer로 판정할 수 있다.
        repository.lockRoomIdForUpdate(reference.roomId());
        ChatRoomOwnership existing = repository.findById(reference.roomId())
            .orElse(null);
        if (existing != null) {
            if (existing.matches(reference)) {
                return existing.toReference();
            }
            throw new IllegalStateException("chat room ownership binding은 변경할 수 없습니다.");
        }

        Optional<Long> ownerTuple = repository.lockOwnerTupleForUpdate(
            reference.namespace(),
            reference.ownerResourceKey(),
            reference.slot()
        );
        if (ownerTuple.isPresent()) {
            throw new IllegalStateException("owner tuple은 다른 chat room에 이미 바인딩되어 있습니다.");
        }

        throw new IllegalStateException("chat room ownership insert가 반영되지 않았습니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomOwnerReference> findByRoomId(Long roomId) {
        return repository.findById(roomId).map(ChatRoomOwnership::toReference);
    }

    @Override
    @Transactional
    public Optional<ChatRoomOwnerReference> findByRoomIdForUpdate(Long roomId) {
        repository.lockRoomIdForUpdate(roomId);
        return repository.findById(roomId).map(ChatRoomOwnership::toReference);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findDistinctNamespaces() {
        return repository.findDistinctNamespaces();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatRoomOwnerReference> findByOwner(
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        return repository.findByNamespaceAndOwnerResourceKeyAndSlot(
            namespace,
            ownerResourceKey,
            slot
        ).map(ChatRoomOwnership::toReference);
    }

    public long count() {
        return repository.count();
    }
}
