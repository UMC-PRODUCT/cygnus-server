package com.umc.product.community.application.service.message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.umc.product.chat.adapter.out.persistence.ChatMessageJpaRepository;
import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.JoinChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.JoinChatRoomCommand;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.out.LoadChatMemberPort;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.port.in.command.thread.LeaveCommunityThreadUseCase;
import com.umc.product.community.application.port.in.command.thread.message.CreateCommunityThreadMessageUseCase;
import com.umc.product.community.application.port.in.command.thread.message.UpdateCommunityThreadReadUseCase;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.global.event.adapter.out.persistence.EventOutboxJpaRepository;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.concurrency.PostgreSqlTransactionRace;

abstract class CommunityThreadConcurrencyIntegrationSupport extends IntegrationTestSupport {

    static final Long OWNER_ID = 10L;
    static final Long MEMBER_ID = 20L;
    static final long TIMEOUT_SECONDS = 10L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Autowired
    CreateChatRoomUseCase createChatRoomUseCase;
    @Autowired
    JoinChatRoomUseCase joinChatRoomUseCase;
    @Autowired
    CreateChatMessageUseCase createChatMessageUseCase;
    @Autowired
    CreateCommunityThreadMessageUseCase createThreadMessageUseCase;
    @Autowired
    UpdateCommunityThreadReadUseCase updateThreadReadUseCase;
    @Autowired
    LeaveCommunityThreadUseCase leaveThreadUseCase;
    @Autowired
    ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;
    @Autowired
    LoadChatMemberPort loadChatMemberPort;
    @Autowired
    LoadCommunityThreadMemberPort loadThreadMemberPort;
    @Autowired
    SaveCommunityThreadPort saveThreadPort;
    @Autowired
    SaveCommunityThreadMemberPort saveThreadMemberPort;
    @Autowired
    ChatMessageJpaRepository chatMessageRepository;
    @Autowired
    EventOutboxJpaRepository eventOutboxRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    JdbcTemplate jdbcTemplate;

    PostgreSqlTransactionRace race;

    @BeforeEach
    void setUpRace() {
        race = new PostgreSqlTransactionRace(transactionManager, jdbcTemplate);
    }

    @AfterEach
    void closeRace() {
        race.close();
    }

    ThreadContext createThread() {
        Long roomId = createChatRoomUseCase.create(CreateChatRoomCommand.from(OWNER_ID)).roomId();
        joinChatRoomUseCase.joinChatRoom(new JoinChatRoomCommand(roomId, MEMBER_ID));
        CommunityThread thread = saveThreadPort.save(CommunityThread.create(
            roomId,
            "동시성 스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            OWNER_ID,
            NOW
        ));
        saveThreadMemberPort.saveAll(List.of(
            CommunityThreadMember.createOwner(thread.getId(), OWNER_ID, NOW),
            CommunityThreadMember.createMember(thread.getId(), MEMBER_ID, NOW)
        ));
        return new ThreadContext(thread.getId(), roomId);
    }

    CreateCommunityThreadMessageCommand communityText(
        ThreadContext thread,
        Long senderId,
        UUID clientMessageId
    ) {
        return new CreateCommunityThreadMessageCommand(
            thread.threadId(),
            senderId,
            clientMessageId,
            CommunityThreadMessageType.TEXT,
            "동시성 메시지",
            List.of(),
            List.of(),
            null
        );
    }

    CreateChatMessageCommand chatText(ThreadContext thread, UUID clientMessageId) {
        return new CreateChatMessageCommand(
            thread.roomId(),
            OWNER_ID,
            clientMessageId,
            MessageContentType.TEXT,
            "watermark 이후",
            List.of(),
            List.of(),
            null
        );
    }

    CommunityThreadMember threadMember(Long threadId, Long memberId) {
        return loadThreadMemberPort.findByThreadIdAndMemberId(threadId, memberId).orElseThrow();
    }

    long countEvent(String eventType) {
        return eventOutboxRepository.findAll().stream()
            .filter(outbox -> eventType.equals(outbox.getEventType()))
            .count();
    }

    record ThreadContext(Long threadId, Long roomId) {
    }
}
