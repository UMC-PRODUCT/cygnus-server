package com.umc.product.chat.application.service;

import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.storage.domain.FileUsageCoordinate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageAttachmentUsageService {

    private static final String NAMESPACE = "chat.message";
    private static final String SLOT = "attachments";

    private final ManageFileUsageUseCase manageFileUsageUseCase;
    private final LoadChatMessagePort loadChatMessagePort;

    public void synchronize(ChatMessage message, Long requesterMemberId) {
        manageFileUsageUseCase.replaceUsages(new ReplaceFileUsagesCommand(
            coordinate(message.getId()),
            new LinkedHashSet<>(message.getFileMetadataIds()),
            requesterMemberId
        ));
    }

    public void detachByRoomId(Long roomId) {
        List<Long> messageIds = loadChatMessagePort.listIdsByRoomId(roomId);
        if (messageIds.isEmpty()) {
            return;
        }
        List<FileUsageCoordinate> coordinates = messageIds.stream()
            .map(ChatMessageAttachmentUsageService::coordinate)
            .toList();
        manageFileUsageUseCase.removeAll(new BulkRemoveFileUsagesCommand(coordinates));
    }

    private static FileUsageCoordinate coordinate(Long messageId) {
        return FileUsageCoordinate.of(NAMESPACE, messageId.toString(), SLOT);
    }
}
