package com.umc.product.chat.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.out.LoadChatMessageMentionPort;
import com.umc.product.chat.application.port.out.LoadChatMessagePort;
import com.umc.product.chat.application.port.out.LoadChatMessageReactionPort;
import com.umc.product.chat.application.port.out.dto.ChatReactionSummary;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.MessageContentType;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatMessageInfoAssembler")
class ChatMessageInfoAssemblerTest {

    @Mock
    LoadChatMessagePort loadChatMessagePort;
    @Mock
    LoadChatMessageMentionPort loadChatMessageMentionPort;
    @Mock
    LoadChatMessageReactionPort loadChatMessageReactionPort;

    @InjectMocks
    ChatMessageInfoAssembler sut;

    @Test
    @DisplayName("page의 mention, reaction, reply를 각각 batch 조회해 enriched projection을 만든다")
    void assemble_enrichedPage() {
        ChatMessage reply = message(90L, "가".repeat(101), null);
        ChatMessage first = message(100L, "답장", 90L);
        ChatMessage second = message(101L, "일반", null);
        List<Long> messageIds = List.of(100L, 101L);
        given(loadChatMessageMentionPort.listMemberIdsByMessageIds(messageIds))
            .willReturn(Map.of(100L, List.of(20L, 30L)));
        given(loadChatMessageReactionPort.summarizeByMessageIds(messageIds, 10L))
            .willReturn(List.of(
                new ChatReactionSummary(100L, "👍", 2L, true),
                new ChatReactionSummary(100L, "🎉", 1L, false)
            ));
        given(loadChatMessagePort.listByIds(List.of(90L))).willReturn(List.of(reply));

        List<ChatMessageInfo> result = sut.assemble(List.of(first, second), 10L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).mentionedMemberIds()).containsExactly(20L, 30L);
        assertThat(result.get(0).reactions()).extracting("emoji").containsExactly("👍", "🎉");
        assertThat(result.get(0).reactions().get(0).reactedByMe()).isTrue();
        assertThat(result.get(0).replyTo().messageId()).isEqualTo(90L);
        assertThat(result.get(0).replyTo().senderMemberId()).isEqualTo(20L);
        assertThat(result.get(0).replyTo().snippet()).hasSize(100);
        assertThat(result.get(1).mentionedMemberIds()).isEmpty();
        assertThat(result.get(1).reactions()).isEmpty();
        assertThat(result.get(1).replyTo()).isNull();

        then(loadChatMessageMentionPort).should().listMemberIdsByMessageIds(messageIds);
        then(loadChatMessageReactionPort).should().summarizeByMessageIds(messageIds, 10L);
        then(loadChatMessagePort).should().listByIds(List.of(90L));
    }

    @Test
    @DisplayName("답장이 없는 메시지만 있는 page도 reply 없이 조립한다")
    void assemble_allMessagesHaveNoReply() {
        ChatMessage first = message(100L, "첫 번째 메시지", null);
        ChatMessage second = message(101L, "두 번째 메시지", null);
        List<Long> messageIds = List.of(100L, 101L);
        given(loadChatMessageMentionPort.listMemberIdsByMessageIds(messageIds))
            .willReturn(Map.of());
        given(loadChatMessageReactionPort.summarizeByMessageIds(messageIds, 10L))
            .willReturn(List.of());

        List<ChatMessageInfo> result = sut.assemble(List.of(first, second), 10L);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(info -> assertThat(info.replyTo()).isNull());
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수신자별 reaction은 batch 한 번으로 개인화하고 공통 message 조립 조회는 반복하지 않는다")
    void assembleForViewers_personalizesReactionsWithFixedBatchReads() {
        ChatMessage message = message(100L, "메시지", null);
        List<Long> messageIds = List.of(100L);
        List<Long> viewerMemberIds = List.of(10L, 20L);
        given(loadChatMessageMentionPort.listMemberIdsByMessageIds(messageIds))
            .willReturn(Map.of(100L, List.of(30L)));
        Map<Long, List<ChatReactionSummary>> reactionsByViewer = new LinkedHashMap<>();
        reactionsByViewer.put(10L, List.of(new ChatReactionSummary(100L, "👍", 1L, true)));
        reactionsByViewer.put(20L, List.of(new ChatReactionSummary(100L, "👍", 1L, false)));
        given(loadChatMessageReactionPort.summarizeByMessageIdsForViewers(messageIds, viewerMemberIds))
            .willReturn(reactionsByViewer);

        Map<Long, ChatMessageInfo> result = sut.assembleForViewers(message, viewerMemberIds);

        assertThat(result.keySet()).containsExactly(10L, 20L);
        assertThat(result.get(10L).reactions().get(0).reactedByMe()).isTrue();
        assertThat(result.get(20L).reactions().get(0).reactedByMe()).isFalse();
        assertThat(result.values()).allSatisfy(info ->
            assertThat(info.mentionedMemberIds()).containsExactly(30L));
        then(loadChatMessageMentionPort).should().listMemberIdsByMessageIds(messageIds);
        then(loadChatMessageReactionPort).should()
            .summarizeByMessageIdsForViewers(messageIds, viewerMemberIds);
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("빈 viewer batch는 mention·reaction·reply 조회 없이 빈 결과를 반환한다")
    void assembleForViewers_emptyBatchShortCircuits() {
        assertThat(sut.assembleForViewers(message(100L, "본문", null), List.of())).isEmpty();
        then(loadChatMessageMentionPort).shouldHaveNoInteractions();
        then(loadChatMessageReactionPort).shouldHaveNoInteractions();
        then(loadChatMessagePort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("본문이 없는 IMAGE 답장의 snippet은 null로 유지한다")
    void assemble_replyWithoutContentKeepsNullSnippet() {
        ChatMessage reply = ChatMessage.create(
            1L,
            20L,
            MessageContentType.IMAGE,
            null,
            List.of("file-1")
        );
        ReflectionTestUtils.setField(reply, "id", 90L);
        ChatMessage original = message(100L, "답장", 90L);
        given(loadChatMessageMentionPort.listMemberIdsByMessageIds(List.of(100L)))
            .willReturn(Map.of());
        given(loadChatMessageReactionPort.summarizeByMessageIds(List.of(100L), 10L))
            .willReturn(List.of());
        given(loadChatMessagePort.listByIds(List.of(90L))).willReturn(List.of(reply));

        ChatMessageInfo result = sut.assemble(original, 10L);

        assertThat(result.replyTo()).isNotNull();
        assertThat(result.replyTo().snippet()).isNull();
    }

    private ChatMessage message(Long id, String content, Long replyToId) {
        ChatMessage message = ChatMessage.create(
            1L,
            20L,
            MessageContentType.TEXT,
            content,
            List.of(),
            replyToId
        );
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", Instant.parse("2026-07-18T00:00:00Z"));
        return message;
    }
}
