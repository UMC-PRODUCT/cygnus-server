package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.storage.application.port.in.query.GetFileUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("Community thread recipient message assembler")
class CommunityThreadMessageRecipientAssemblerTest {

    @Mock
    GetMemberUseCase getMemberUseCase;

    @Mock
    GetFileUseCase getFileUseCase;

    @Test
    @DisplayName("수신자별 reaction을 유지하면서 sender 이름은 한 번의 Member batch로 조립한다")
    void assembleForRecipients_loadsMemberNamesOnce() {
        CommunityThreadMessageInfoAssembler sut =
            new CommunityThreadMessageInfoAssembler(getMemberUseCase, getFileUseCase);
        Map<Long, ChatMessageInfo> messagesByRecipient = new LinkedHashMap<>();
        messagesByRecipient.put(10L, message(true));
        messagesByRecipient.put(20L, message(false));
        MemberInfo sender = MemberInfo.builder().id(30L).name("보낸이").build();
        given(getMemberUseCase.findAllByIds(Set.of(30L))).willReturn(Map.of(30L, sender));

        Map<Long, CommunityThreadMessageInfo> result = sut.assembleForRecipients(
            11L,
            messagesByRecipient
        );

        assertThat(result.keySet()).containsExactly(10L, 20L);
        assertThat(result.get(10L).senderName()).isEqualTo("보낸이");
        assertThat(result.get(10L).reactions().get(0).reactedByMe()).isTrue();
        assertThat(result.get(20L).reactions().get(0).reactedByMe()).isFalse();
        then(getMemberUseCase).should().findAllByIds(Set.of(30L));
    }

    private ChatMessageInfo message(boolean reactedByViewer) {
        return new ChatMessageInfo(
            900L,
            101L,
            30L,
            MessageContentType.TEXT,
            "메시지",
            List.of(),
            Instant.parse("2026-07-18T00:00:00Z"),
            null,
            null,
            null,
            null,
            List.of(),
            null,
            List.of(new ChatReactionInfo("👍", 1L, reactedByViewer))
        );
    }
}
