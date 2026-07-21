package com.umc.product.community.application.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.command.CreateChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.EditChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.ManageChatMessageReactionUseCase;
import com.umc.product.chat.application.port.in.command.TombstoneChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.UpdateChatReadUseCase;
import com.umc.product.chat.application.port.in.command.dto.ChangeChatMessageReactionCommand;
import com.umc.product.chat.application.port.in.command.dto.ChatMessageMutationResult;
import com.umc.product.chat.application.port.in.command.dto.ChatReactionMutationResult;
import com.umc.product.chat.application.port.in.command.dto.ChatReadMutationResult;
import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.EditChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.TombstoneChatMessageCommand;
import com.umc.product.chat.application.port.in.command.dto.UpdateChatReadCommand;
import com.umc.product.chat.application.port.in.query.ListChatRoomSummariesUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatMessageInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatReactionInfo;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomSummaryInfo;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.community.application.event.CommunityThreadMentionedEvent;
import com.umc.product.community.application.event.CommunityThreadMessageCreatedEvent;
import com.umc.product.community.application.port.in.command.thread.message.dto.ChangeCommunityThreadMessageReactionCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.CreateCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.EditCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.TombstoneCommunityThreadMessageCommand;
import com.umc.product.community.application.port.in.command.thread.message.dto.UpdateCommunityThreadReadCommand;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadMessageType;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReactionMutationInfo;
import com.umc.product.community.application.port.in.query.thread.message.dto.CommunityThreadReadMutationInfo;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadMessageCommandService")
class CommunityThreadMessageCommandServiceTest {

    private static final Long THREAD_ID = 11L;
    private static final Long ROOM_ID = 101L;
    private static final Long OWNER_ID = 10L;
    private static final Long ADMIN_ID = 20L;
    private static final Long MEMBER_ID = 30L;
    private static final Long LEFT_ID = 40L;
    private static final Instant NOW = Instant.parse("2026-07-18T00:00:00Z");

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadThreadMemberPort;
    @Mock
    SaveCommunityThreadPort saveThreadPort;
    @Mock
    SaveCommunityThreadMemberPort saveThreadMemberPort;
    @Mock
    CreateChatMessageUseCase createChatMessageUseCase;
    @Mock
    EditChatMessageUseCase editChatMessageUseCase;
    @Mock
    TombstoneChatMessageUseCase tombstoneChatMessageUseCase;
    @Mock
    ManageChatMessageReactionUseCase reactionUseCase;
    @Mock
    UpdateChatReadUseCase updateChatReadUseCase;
    @Mock
    ListChatRoomSummariesUseCase listChatRoomSummariesUseCase;
    @Mock
    CommunityThreadMessageInfoAssembler infoAssembler;
    @Mock
    CommunityThreadMessageInfo communityMessageInfo;
    @Mock
    DomainEventPublisher domainEventPublisher;

    @InjectMocks
    CommunityThreadMessageCommandService sut;

    @Test
    @DisplayName("생성은 Community thread lock과 ACTIVE 멤버 검증 뒤 Chat을 호출하고 projection과 두 fact를 한 번 저장한다")
    void create_validatesLockMembershipBeforeChatAndProjectsOnce() {
        CommunityThread thread = thread();
        CommunityThreadMember sender = active(OWNER_ID, CommunityThreadMemberRole.OWNER);
        CommunityThreadMember recipient = active(ADMIN_ID, CommunityThreadMemberRole.ADMIN);
        CommunityThreadMember secondRecipient = active(MEMBER_ID, CommunityThreadMemberRole.MEMBER);
        CommunityThreadMember left = left(LEFT_ID);
        UUID clientMessageId = UUID.fromString("2a7f995e-5e0c-4d3f-9f2f-231d488e7f90");
        ChatMessageInfo chatInfo = chatInfo(900L, OWNER_ID, "새 메시지");
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(sender));
        given(loadThreadMemberPort.listByThreadId(THREAD_ID))
            .willReturn(List.of(sender, recipient, secondRecipient, left));
        given(createChatMessageUseCase.create(any(CreateChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, false));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);
        given(saveThreadPort.save(thread)).willReturn(thread);
        given(saveThreadMemberPort.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        CommunityThreadMessageMutationInfo result = sut.create(new CreateCommunityThreadMessageCommand(
            THREAD_ID,
            OWNER_ID,
            clientMessageId,
            CommunityThreadMessageType.TEXT,
            "새 메시지",
            List.of(),
            List.of(ADMIN_ID, ADMIN_ID),
            null
        ));

        assertThat(result.deduplicated()).isFalse();
        assertThat(thread.getLastMessageId()).isEqualTo(chatInfo.messageId());
        assertThat(thread.getLastMessagePreview()).isEqualTo(chatInfo.content());
        assertThat(thread.getLastMessageSenderMemberId()).isEqualTo(OWNER_ID);
        assertThat(sender.getUnreadCount()).isZero();
        assertThat(recipient.getUnreadCount()).isEqualTo(1L);
        assertThat(secondRecipient.getUnreadCount()).isEqualTo(1L);
        assertThat(left.getUnreadCount()).isZero();

        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, createChatMessageUseCase,
            saveThreadPort, saveThreadMemberPort, domainEventPublisher);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, OWNER_ID);
        order.verify(createChatMessageUseCase).create(any(CreateChatMessageCommand.class));
        order.verify(saveThreadPort).save(thread);
        order.verify(saveThreadMemberPort).saveAll(any());
        order.verify(domainEventPublisher).publishAll(any(Collection.class));

        ArgumentCaptor<Collection> facts = ArgumentCaptor.forClass(Collection.class);
        then(domainEventPublisher).should().publishAll(facts.capture());
        Collection<?> publishedFacts = facts.getValue();
        assertThat(publishedFacts).hasSize(2);
        assertThat(publishedFacts).anyMatch(fact -> fact instanceof CommunityThreadMessageCreatedEvent);
        assertThat(publishedFacts).anyMatch(fact -> fact instanceof CommunityThreadMentionedEvent);
        CommunityThreadMessageCreatedEvent created = publishedFacts.stream()
            .filter(CommunityThreadMessageCreatedEvent.class::isInstance)
            .map(CommunityThreadMessageCreatedEvent.class::cast)
            .findFirst()
            .orElseThrow();
        assertThat(created.threadId()).isEqualTo(THREAD_ID);
        assertThat(created.messageId()).isEqualTo(chatInfo.messageId());
        assertThat(created.senderMemberId()).isEqualTo(OWNER_ID);
        assertThat(created.recipientMemberIds()).containsExactly(ADMIN_ID, MEMBER_ID);
        CommunityThreadMentionedEvent mentioned = publishedFacts.stream()
            .filter(CommunityThreadMentionedEvent.class::isInstance)
            .map(CommunityThreadMentionedEvent.class::cast)
            .findFirst()
            .orElseThrow();
        assertThat(mentioned.mentionedMemberIds()).containsExactly(ADMIN_ID);
    }

    @Test
    @DisplayName("동일 clientMessageId 재생은 Chat deduplicated 결과만 반환하고 projection·save·fact를 만들지 않는다")
    void create_replayDoesNotTouchCommunityProjection() {
        CommunityThread thread = thread();
        thread.updateLastMessage(800L, "기존", OWNER_ID, NOW.minusSeconds(5));
        CommunityThreadMember sender = active(OWNER_ID, CommunityThreadMemberRole.OWNER);
        UUID clientMessageId = UUID.fromString("b5ad7d54-0f48-4a8a-8e80-cfa82905a04f");
        ChatMessageInfo chatInfo = chatInfo(800L, OWNER_ID, "기존");
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(sender));
        given(createChatMessageUseCase.create(any(CreateChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, true));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);

        CommunityThreadMessageMutationInfo result = sut.create(new CreateCommunityThreadMessageCommand(
            THREAD_ID,
            OWNER_ID,
            clientMessageId,
            CommunityThreadMessageType.TEXT,
            "기존",
            List.of(),
            List.of(),
            null
        ));

        assertThat(result.deduplicated()).isTrue();
        then(saveThreadPort).shouldHaveNoInteractions();
        then(saveThreadMemberPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
        assertThat(thread.getLastMessageId()).isEqualTo(800L);
    }

    @Test
    @DisplayName("멘션이 없는 fresh create는 message-created fact만 한 번 발행한다")
    void create_withoutMentionsPublishesOnlyCreatedFact() {
        CommunityThread thread = thread();
        CommunityThreadMember sender = active(OWNER_ID, CommunityThreadMemberRole.OWNER);
        CommunityThreadMember recipient = active(MEMBER_ID, CommunityThreadMemberRole.MEMBER);
        ChatMessageInfo chatInfo = chatInfo(901L, OWNER_ID, "멘션 없음");
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(sender));
        given(loadThreadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(sender, recipient));
        given(createChatMessageUseCase.create(any(CreateChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, false));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);
        given(saveThreadPort.save(thread)).willReturn(thread);
        given(saveThreadMemberPort.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

        sut.create(createCommand(OWNER_ID));

        ArgumentCaptor<Collection> facts = ArgumentCaptor.forClass(Collection.class);
        then(domainEventPublisher).should().publishAll(facts.capture());
        Collection<?> publishedFacts = facts.getValue();
        assertThat(publishedFacts).singleElement()
            .isInstanceOf(CommunityThreadMessageCreatedEvent.class);
    }

    @Test
    @DisplayName("삭제된 thread는 Chat mutation 전에 THREAD_NOT_FOUND로 거절한다")
    void deletedThreadIsRejectedBeforeChat() {
        CommunityThread deleted = thread();
        deleted.delete(NOW);
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> sut.create(createCommand(OWNER_ID)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_NOT_FOUND);
        then(loadThreadMemberPort).shouldHaveNoInteractions();
        then(createChatMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("없는 thread 멤버는 THREAD_MEMBER_NOT_FOUND로 거절하고 Chat을 호출하지 않는다")
    void missingMemberIsRejectedBeforeChat() {
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.create(createCommand(OWNER_ID)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_MEMBER_NOT_FOUND);
        then(createChatMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("LEFT 멤버는 THREAD_ACCESS_DENIED로 거절하고 Chat을 호출하지 않는다")
    void inactiveMemberIsRejectedBeforeChat() {
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(left(OWNER_ID)));

        assertThatThrownBy(() -> sut.create(createCommand(OWNER_ID)))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(createChatMessageUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수정은 thread lock과 멤버 검증 뒤 Chat을 호출하고 마지막 메시지 projection만 갱신한다")
    void editRefreshesLastMessageProjectionAfterLock() {
        CommunityThread thread = thread();
        thread.updateLastMessage(900L, "이전", OWNER_ID, NOW.minusSeconds(10));
        CommunityThreadMember sender = active(OWNER_ID, CommunityThreadMemberRole.OWNER);
        ChatMessageInfo chatInfo = chatInfo(900L, OWNER_ID, "수정");
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(sender));
        given(editChatMessageUseCase.edit(any(EditChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, false));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);
        given(saveThreadPort.save(thread)).willReturn(thread);

        CommunityThreadMessageMutationInfo result = sut.edit(
            new EditCommunityThreadMessageCommand(THREAD_ID, 900L, OWNER_ID, "수정")
        );

        assertThat(result.deduplicated()).isFalse();
        assertThat(thread.getLastMessagePreview()).isEqualTo("수정");
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, editChatMessageUseCase, saveThreadPort);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, OWNER_ID);
        order.verify(editChatMessageUseCase).edit(any(EditChatMessageCommand.class));
        order.verify(saveThreadPort).save(thread);
    }

    @Test
    @DisplayName("동일 내용 수정 재시도는 deduplicated 결과를 반환하고 projection save를 생략한다")
    void edit_deduplicatedSkipsProjectionSave() {
        CommunityThread thread = thread();
        thread.updateLastMessage(900L, "동일", OWNER_ID, NOW.minusSeconds(10));
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID))
            .willReturn(Optional.of(active(OWNER_ID, CommunityThreadMemberRole.OWNER)));
        ChatMessageInfo chatInfo = chatInfo(900L, OWNER_ID, "동일");
        given(editChatMessageUseCase.edit(any(EditChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, true));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);

        CommunityThreadMessageMutationInfo result = sut.edit(
            new EditCommunityThreadMessageCommand(THREAD_ID, 900L, OWNER_ID, "동일")
        );

        assertThat(result.deduplicated()).isTrue();
        then(saveThreadPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("tombstone moderator는 요청 body가 아니라 Community OWNER·ADMIN 역할에서만 파생된다")
    void tombstoneModeratorIsDerivedFromCommunityRole() {
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(
            Optional.of(active(OWNER_ID, CommunityThreadMemberRole.OWNER))
        );
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, ADMIN_ID)).willReturn(
            Optional.of(active(ADMIN_ID, CommunityThreadMemberRole.ADMIN))
        );
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID)).willReturn(
            Optional.of(active(MEMBER_ID, CommunityThreadMemberRole.MEMBER))
        );
        ChatMessageInfo chatInfo = chatInfo(900L, OWNER_ID, "삭제됨");
        given(tombstoneChatMessageUseCase.tombstone(any(TombstoneChatMessageCommand.class)))
            .willReturn(new ChatMessageMutationResult(chatInfo, false));
        given(infoAssembler.assemble(THREAD_ID, chatInfo)).willReturn(communityMessageInfo);

        sut.tombstone(new TombstoneCommunityThreadMessageCommand(THREAD_ID, 900L, OWNER_ID));
        sut.tombstone(new TombstoneCommunityThreadMessageCommand(THREAD_ID, 900L, ADMIN_ID));
        sut.tombstone(new TombstoneCommunityThreadMessageCommand(THREAD_ID, 900L, MEMBER_ID));

        ArgumentCaptor<TombstoneChatMessageCommand> commands =
            ArgumentCaptor.forClass(TombstoneChatMessageCommand.class);
        then(tombstoneChatMessageUseCase).should(org.mockito.Mockito.times(3)).tombstone(commands.capture());
        assertThat(commands.getAllValues()).extracting(TombstoneChatMessageCommand::moderator)
            .containsExactly(true, true, false);
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, tombstoneChatMessageUseCase);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, OWNER_ID);
        order.verify(tombstoneChatMessageUseCase).tombstone(any(TombstoneChatMessageCommand.class));
    }

    @Test
    @DisplayName("reaction은 Community thread lock과 ACTIVE 멤버 검증 뒤 Chat public UseCase를 호출한다")
    void reactionUsesCommunityLockBeforeChat() {
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(active(MEMBER_ID, CommunityThreadMemberRole.MEMBER)));
        given(reactionUseCase.add(any(ChangeChatMessageReactionCommand.class))).willReturn(
            new ChatReactionMutationResult(900L, List.of(new ChatReactionInfo("👍", 2L, true)), false)
        );

        CommunityThreadReactionMutationInfo result = sut.add(
            new ChangeCommunityThreadMessageReactionCommand(THREAD_ID, 900L, MEMBER_ID, "👍")
        );

        assertThat(result.deduplicated()).isFalse();
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, reactionUseCase);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID);
        order.verify(reactionUseCase).add(any(ChangeChatMessageReactionCommand.class));
    }

    @Test
    @DisplayName("reaction remove도 동일한 Community lock·멤버 검증 뒤 Chat public UseCase를 호출한다")
    void reactionRemoveUsesCommunityLockBeforeChat() {
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(active(MEMBER_ID, CommunityThreadMemberRole.MEMBER)));
        given(reactionUseCase.remove(any(ChangeChatMessageReactionCommand.class))).willReturn(
            new ChatReactionMutationResult(900L, List.of(), false)
        );

        CommunityThreadReactionMutationInfo result = sut.remove(
            new ChangeCommunityThreadMessageReactionCommand(THREAD_ID, 900L, MEMBER_ID, "👍")
        );

        assertThat(result.deduplicated()).isFalse();
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, reactionUseCase);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID);
        order.verify(reactionUseCase).remove(any(ChangeChatMessageReactionCommand.class));
    }

    @Test
    @DisplayName("read는 Chat watermark 이후 정확한 unread summary를 조회하고 projection이 다를 때만 저장한다")
    void readRefreshesExactUnreadProjection() {
        CommunityThread thread = thread();
        CommunityThreadMember member = active(MEMBER_ID, CommunityThreadMemberRole.MEMBER);
        member.updateUnreadCount(7L);
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(member));
        given(updateChatReadUseCase.update(any(UpdateChatReadCommand.class)))
            .willReturn(new ChatReadMutationResult(ROOM_ID, MEMBER_ID, 900L, false));
        given(listChatRoomSummariesUseCase.listRoomSummaries(MEMBER_ID, List.of(ROOM_ID)))
            .willReturn(List.of(new ChatRoomSummaryInfo(ROOM_ID, null, 2L)));
        given(saveThreadMemberPort.save(member)).willReturn(member);

        CommunityThreadReadMutationInfo result = sut.update(
            new UpdateCommunityThreadReadCommand(THREAD_ID, MEMBER_ID, 900L)
        );

        assertThat(result.lastReadMessageId()).isEqualTo(900L);
        assertThat(member.getUnreadCount()).isEqualTo(2L);
        InOrder order = inOrder(loadThreadPort, loadThreadMemberPort, updateChatReadUseCase,
            listChatRoomSummariesUseCase, saveThreadMemberPort);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadThreadMemberPort).findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID);
        order.verify(updateChatReadUseCase).update(any(UpdateChatReadCommand.class));
        order.verify(listChatRoomSummariesUseCase).listRoomSummaries(MEMBER_ID, List.of(ROOM_ID));
        order.verify(saveThreadMemberPort).save(member);
    }

    @Test
    @DisplayName("non-advancing read dedup이라도 unread projection이 이미 정확하면 추가 save와 fact를 만들지 않는다")
    void read_deduplicatedWithMatchingProjectionSkipsSave() {
        CommunityThreadMember member = active(MEMBER_ID, CommunityThreadMemberRole.MEMBER);
        member.updateUnreadCount(2L);
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread()));
        given(loadThreadMemberPort.findByThreadIdAndMemberId(THREAD_ID, MEMBER_ID))
            .willReturn(Optional.of(member));
        given(updateChatReadUseCase.update(any(UpdateChatReadCommand.class)))
            .willReturn(new ChatReadMutationResult(ROOM_ID, MEMBER_ID, 900L, true));
        given(listChatRoomSummariesUseCase.listRoomSummaries(MEMBER_ID, List.of(ROOM_ID)))
            .willReturn(List.of(new ChatRoomSummaryInfo(ROOM_ID, null, 2L)));

        CommunityThreadReadMutationInfo result = sut.update(
            new UpdateCommunityThreadReadCommand(THREAD_ID, MEMBER_ID, 900L)
        );

        assertThat(result.deduplicated()).isTrue();
        then(saveThreadMemberPort).shouldHaveNoInteractions();
        then(domainEventPublisher).shouldHaveNoInteractions();
    }

    private CreateCommunityThreadMessageCommand createCommand(Long memberId) {
        return new CreateCommunityThreadMessageCommand(
            THREAD_ID,
            memberId,
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            CommunityThreadMessageType.TEXT,
            "본문",
            List.of(),
            List.of(),
            null
        );
    }

    private CommunityThread thread() {
        CommunityThread thread = CommunityThread.create(
            ROOM_ID,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            OWNER_ID,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", THREAD_ID);
        return thread;
    }

    private CommunityThreadMember active(Long memberId, CommunityThreadMemberRole role) {
        CommunityThreadMember member = role == CommunityThreadMemberRole.OWNER
            ? CommunityThreadMember.createOwner(THREAD_ID, memberId, NOW)
            : CommunityThreadMember.createMember(THREAD_ID, memberId, NOW);
        if (role == CommunityThreadMemberRole.ADMIN) {
            member.changeRole(CommunityThreadMemberRole.ADMIN);
        }
        return member;
    }

    private CommunityThreadMember left(Long memberId) {
        CommunityThreadMember member = active(memberId, CommunityThreadMemberRole.MEMBER);
        member.leave(NOW.minusSeconds(60));
        return member;
    }

    private ChatMessageInfo chatInfo(Long messageId, Long senderId, String content) {
        return new ChatMessageInfo(
            messageId,
            ROOM_ID,
            senderId,
            MessageContentType.TEXT,
            content,
            List.of(),
            NOW,
            null,
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            null,
            null,
            List.of(),
            null,
            List.of()
        );
    }
}
