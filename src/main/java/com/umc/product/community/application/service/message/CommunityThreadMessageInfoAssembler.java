package com.umc.product.community.application.service.message;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageReplyInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageFileInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMentionInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageReplyInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageStatus;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;
import com.umc.product.storage.application.port.in.query.dto.FileInfo;

import lombok.RequiredArgsConstructor;

/**
 * Chat 엔진의 조회 모델을 Community thread 공개 모델로 변환한다.
 *
 * <p>Chat room id는 Community 외부 계약에 포함하지 않고, 이름이 필요한 멤버와 첨부 파일을
 * 각각 한 번의 batch query로 조립한다.</p>
 *
 * <p>첨부 파일은 fileId 만으로는 렌더링할 수 없으므로 storage 도메인에서 접근 URL 을 조회해
 * 함께 내려준다. REST 응답과 STOMP 브로드캐스트가 이 조립 결과를 공유한다.</p>
 */
@Component
@RequiredArgsConstructor
public class CommunityThreadMessageInfoAssembler {

    private static final String UNKNOWN_MEMBER_NAME = "알 수 없음";

    private final GetMemberUseCase getMemberUseCase;
    private final GetFileUseCase getFileUseCase;

    public CommunityThreadMessageInfo assemble(Long threadId, ChatMessageInfo message) {
        return assemble(threadId, List.of(message)).get(0);
    }

    public List<CommunityThreadMessageInfo> assemble(Long threadId, List<ChatMessageInfo> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        Map<Long, MemberInfo> members = loadMembers(messages);
        Map<String, FileInfo> files = loadFiles(messages);
        return messages.stream()
            .map(message -> toInfo(threadId, message, members, files))
            .toList();
    }

    public Map<Long, CommunityThreadMessageInfo> assembleForRecipients(
        Long threadId,
        Map<Long, ChatMessageInfo> messagesByRecipient
    ) {
        if (messagesByRecipient == null || messagesByRecipient.isEmpty()) {
            return Map.of();
        }

        List<ChatMessageInfo> messages = List.copyOf(messagesByRecipient.values());
        Map<Long, MemberInfo> members = loadMembers(messages);
        Map<String, FileInfo> files = loadFiles(messages);
        Map<Long, CommunityThreadMessageInfo> result = new LinkedHashMap<>();
        messagesByRecipient.forEach((recipientMemberId, message) ->
            result.put(recipientMemberId, toInfo(threadId, message, members, files)));
        return Collections.unmodifiableMap(result);
    }

    private Map<Long, MemberInfo> loadMembers(List<ChatMessageInfo> messages) {
        Set<Long> memberIds = new LinkedHashSet<>();
        messages.forEach(message -> {
            if (message.senderMemberId() != null) {
                memberIds.add(message.senderMemberId());
            }
            if (message.mentionedMemberIds() != null) {
                memberIds.addAll(message.mentionedMemberIds());
            }
            ChatMessageReplyInfo reply = message.replyTo();
            if (reply != null && reply.senderMemberId() != null) {
                memberIds.add(reply.senderMemberId());
            }
        });
        return memberIds.isEmpty() ? Map.of() : getMemberUseCase.findAllByIds(memberIds);
    }

    /**
     * 조립 대상 메시지 전체의 fileId 를 모아 storage 도메인에서 IN 쿼리 1회로 batch 조회한다.
     *
     * <p>storage 에서 누락된 fileId 는 결과 Map 에서 빠지므로, 호출부는 누락 가능성을 가정해야 한다.</p>
     */
    private Map<String, FileInfo> loadFiles(List<ChatMessageInfo> messages) {
        Set<String> fileIds = new LinkedHashSet<>();
        messages.forEach(message -> {
            if (message.fileMetadataIds() != null) {
                fileIds.addAll(message.fileMetadataIds());
            }
        });
        return fileIds.isEmpty() ? Map.of() : getFileUseCase.findAllByIds(List.copyOf(fileIds));
    }

    private CommunityThreadMessageInfo toInfo(
        Long threadId,
        ChatMessageInfo message,
        Map<Long, MemberInfo> members,
        Map<String, FileInfo> files
    ) {
        List<CommunityThreadMessageMentionInfo> mentions = message.mentionedMemberIds().stream()
            .map(memberId -> new CommunityThreadMessageMentionInfo(memberId, memberName(members, memberId)))
            .toList();

        CommunityThreadMessageReplyInfo replyTo = toReplyInfo(message.replyTo(), members);
        List<CommunityThreadReactionInfo> reactions = message.reactions().stream()
            .map(this::toReactionInfo)
            .toList();

        return new CommunityThreadMessageInfo(
            message.messageId(),
            threadId,
            message.senderMemberId(),
            memberName(members, message.senderMemberId()),
            message.content(),
            toCommunityType(message.contentType()),
            CommunityThreadMessageStatus.SENT,
            toFileInfos(message.fileMetadataIds(), files),
            mentions,
            replyTo,
            reactions,
            message.clientMessageId(),
            message.createdAt(),
            message.editedAt(),
            message.deletedAt()
        );
    }

    /**
     * 메시지가 참조하는 fileId 순서를 그대로 유지하며 조회 결과를 채운다.
     *
     * <p>storage 에서 누락된 fileId 는 건너뛴다. 원본 순서를 순회하므로 누락이 있어도 남은 파일의
     * 표시 순서는 어긋나지 않는다.</p>
     */
    private List<CommunityThreadMessageFileInfo> toFileInfos(
        List<String> fileMetadataIds,
        Map<String, FileInfo> files
    ) {
        if (fileMetadataIds == null || fileMetadataIds.isEmpty()) {
            return List.of();
        }
        return fileMetadataIds.stream()
            .map(files::get)
            .filter(Objects::nonNull)
            .map(CommunityThreadMessageFileInfo::from)
            .toList();
    }

    private CommunityThreadMessageReplyInfo toReplyInfo(
        ChatMessageReplyInfo reply,
        Map<Long, MemberInfo> members
    ) {
        if (reply == null) {
            return null;
        }
        return new CommunityThreadMessageReplyInfo(
            reply.messageId(),
            memberName(members, reply.senderMemberId()),
            reply.snippet()
        );
    }

    private CommunityThreadReactionInfo toReactionInfo(ChatReactionInfo reaction) {
        return new CommunityThreadReactionInfo(
            reaction.emoji(),
            reaction.count(),
            reaction.reactedByMe()
        );
    }

    private CommunityThreadMessageType toCommunityType(MessageContentType contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        return switch (contentType) {
            case TEXT -> CommunityThreadMessageType.TEXT;
            case IMAGE, FILE -> CommunityThreadMessageType.IMAGE;
            case SYSTEM -> CommunityThreadMessageType.SYSTEM;
        };
    }

    private String memberName(Map<Long, MemberInfo> members, Long memberId) {
        MemberInfo member = members.get(memberId);
        return member == null || member.name() == null ? UNKNOWN_MEMBER_NAME : member.name();
    }
}
