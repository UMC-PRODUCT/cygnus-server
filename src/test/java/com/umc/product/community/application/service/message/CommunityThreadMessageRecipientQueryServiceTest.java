package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.query.GetChatMessageForViewersUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessageUseCase;
import com.umc.product.chat.application.port.in.query.GetChatMessagesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.GetChatMessageForViewersQuery;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageRecipientsQuery;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.service.realtime.CommunityThreadRealtimeMetrics;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread recipient message query service")
class CommunityThreadMessageRecipientQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadThreadMemberPort;
    @Mock
    GetChatMessagesUseCase getChatMessagesUseCase;
    @Mock
    GetChatMessageUseCase getChatMessageUseCase;
    @Mock
    GetChatMessageForViewersUseCase getChatMessageForViewersUseCase;
    @Mock
    CommunityThreadMessageInfoAssembler infoAssembler;
    @Mock
    CommunityThreadRealtimeMetrics realtimeMetrics;

    @InjectMocks
    CommunityThreadMessageQueryService sut;

    @Test
    @DisplayName("ACTIVE 수신자를 batch 검증하고 Chat과 Community 조립을 이벤트당 한 번만 호출한다")
    void getMessageForRecipients_usesOneBatchPerBoundary() {
        List<Long> recipients = List.of(10L, 20L);
        Set<Long> recipientSet = Set.of(10L, 20L);
        Map<Long, ChatMessageInfo> chatMessages = new LinkedHashMap<>();
        chatMessages.put(10L, org.mockito.Mockito.mock(ChatMessageInfo.class));
        chatMessages.put(20L, org.mockito.Mockito.mock(ChatMessageInfo.class));
        Map<Long, CommunityThreadMessageInfo> expected = new LinkedHashMap<>();
        expected.put(10L, org.mockito.Mockito.mock(CommunityThreadMessageInfo.class));
        expected.put(20L, org.mockito.Mockito.mock(CommunityThreadMessageInfo.class));
        given(loadThreadPort.findById(11L)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.listByThreadIdAndMemberIds(11L, recipientSet))
            .willReturn(List.of(activeMember(10L), activeMember(20L)));
        given(getChatMessageForViewersUseCase.getMessageForViewers(
            new GetChatMessageForViewersQuery(101L, 900L, recipients)
        )).willReturn(chatMessages);
        given(infoAssembler.assembleForRecipients(11L, chatMessages)).willReturn(expected);

        Map<Long, CommunityThreadMessageInfo> result = sut.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, recipients)
        );

        assertThat(result).isSameAs(expected);
        then(loadThreadMemberPort).should().listByThreadIdAndMemberIds(11L, recipientSet);
        then(getChatMessageForViewersUseCase).should().getMessageForViewers(
            new GetChatMessageForViewersQuery(101L, 900L, recipients)
        );
        then(infoAssembler).should().assembleForRecipients(11L, chatMessages);
    }

    @Test
    @DisplayName("수신자 중 비ACTIVE 멤버가 있으면 Chat public query 전에 접근을 거절한다")
    void getMessageForRecipients_rejectsInactiveRecipientBeforeChat() {
        CommunityThreadMember left = activeMember(20L);
        left.leave(NOW.plusSeconds(1));
        given(loadThreadPort.findById(11L)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.listByThreadIdAndMemberIds(11L, Set.of(10L, 20L)))
            .willReturn(List.of(activeMember(10L), left));

        assertThatThrownBy(() -> sut.getMessageForRecipients(
            new CommunityThreadMessageRecipientsQuery(11L, 900L, List.of(10L, 20L))
        ))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);

        then(getChatMessageForViewersUseCase).shouldHaveNoInteractions();
        then(infoAssembler).shouldHaveNoInteractions();
    }

    private CommunityThread thread() {
        CommunityThread thread = CommunityThread.create(
            101L,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            10L,
            501L,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", 11L);
        return thread;
    }

    private CommunityThreadMember activeMember(Long memberId) {
        return CommunityThreadMember.createMember(11L, memberId, NOW);
    }
}
