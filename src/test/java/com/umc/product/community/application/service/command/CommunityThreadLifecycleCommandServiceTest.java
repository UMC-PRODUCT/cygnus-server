package com.umc.product.community.application.service.command;

import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.CHAT_ROOM_ID;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.GISU_ID;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.NOW;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.OWNER_ID;
import static com.umc.product.community.application.service.command.CommunityThreadLifecycleTestFixtures.THREAD_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.DeleteChatRoomUseCase;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CommunityThreadLifecycleInfo;
import com.umc.product.community.application.port.in.command.thread.dto.CreateCommunityThreadCommand;
import com.umc.product.community.application.port.in.command.thread.dto.ThreadActorCommand;
import com.umc.product.community.application.port.in.command.thread.dto.UpdateCommunityThreadCommand;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.LoadCommunityThreadPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadMemberPort;
import com.umc.product.community.application.port.out.thread.SaveCommunityThreadPort;
import com.umc.product.community.domain.CommunityThread;
import com.umc.product.community.domain.CommunityThreadMember;
import com.umc.product.community.domain.CommunityThreadProperties;
import com.umc.product.community.domain.enums.CommunityThreadCategory;
import com.umc.product.community.domain.enums.CommunityThreadMemberRole;
import com.umc.product.community.domain.event.CommunityThreadDeletedEvent;
import com.umc.product.community.domain.event.CommunityThreadInvitedEvent;
import com.umc.product.community.domain.event.CommunityThreadUpdatedEvent;
import com.umc.product.community.domain.exception.CommunityDomainException;
import com.umc.product.community.domain.exception.CommunityErrorCode;
import com.umc.product.global.event.application.port.out.DomainEventPublisher;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommunityThreadLifecycleCommandService")
class CommunityThreadLifecycleCommandServiceTest {

    @Mock
    LoadCommunityThreadPort loadThreadPort;
    @Mock
    SaveCommunityThreadPort saveThreadPort;
    @Mock
    LoadCommunityThreadMemberPort loadMemberPort;
    @Mock
    SaveCommunityThreadMemberPort saveMemberPort;
    @Mock
    GetGisuUseCase getGisuUseCase;
    @Mock
    CreateChatRoomUseCase createChatRoomUseCase;
    @Mock
    CommunityThreadInviteManager inviteManager;
    @Mock
    DomainEventPublisher eventPublisher;

    CommunityThreadLifecycleCommandService sut;

    @BeforeEach
    void setUp() {
        sut = new CommunityThreadLifecycleCommandService(
            loadThreadPort,
            saveThreadPort,
            loadMemberPort,
            saveMemberPort,
            getGisuUseCase,
            createChatRoomUseCase,
            inviteManager,
            eventPublisher,
            new CommunityThreadProperties(100),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Chat 소유자 생성 후 Community 스레드와 소유자를 저장하고 적격 초대자를 참여시킨다")
    void create_createsChatThenCommunityOwnerAndInvitees() {
        // given
        CreateCommunityThreadCommand command = new CreateCommunityThreadCommand(
            OWNER_ID,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            List.of(30L, 20L)
        );
        CommunityThreadMember invited20 = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        CommunityThreadMember invited30 = CommunityThreadLifecycleTestFixtures.activeMember(
            30L,
            CommunityThreadMemberRole.MEMBER
        );
        given(getGisuUseCase.getActiveGisuId()).willReturn(GISU_ID);
        given(createChatRoomUseCase.create(any())).willReturn(
            new ChatRoomInfo(100L, NOW, null, List.of(OWNER_ID))
        );
        given(saveThreadPort.save(any())).willAnswer(invocation -> {
            CommunityThread thread = invocation.getArgument(0);
            ReflectionTestUtils.setField(thread, "id", THREAD_ID);
            return thread;
        });
        given(saveMemberPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(inviteManager.invite(any(), any(), any())).willReturn(List.of(invited20, invited30));

        // when
        CommunityThreadLifecycleInfo result = sut.create(command);

        // then
        assertThat(result.threadId()).isEqualTo(THREAD_ID);
        assertThat(result.memberCount()).isEqualTo(3L);
        InOrder order = Mockito.inOrder(
            getGisuUseCase,
            createChatRoomUseCase,
            saveThreadPort,
            saveMemberPort,
            inviteManager,
            eventPublisher
        );
        order.verify(getGisuUseCase).getActiveGisuId();
        order.verify(createChatRoomUseCase).create(any());
        order.verify(saveThreadPort).save(any());
        order.verify(saveMemberPort).save(any());
        order.verify(inviteManager).invite(any(), Mockito.eq(List.of(30L, 20L)), Mockito.eq(NOW));
        order.verify(eventPublisher).publish(any(CommunityThreadInvitedEvent.class));
    }

    @Test
    @DisplayName("설정 용량을 넘는 생성 초대는 Chat 방 생성 전에 거절한다")
    void create_rejectsCapacityBeforeChatCreation() {
        // given
        sut = new CommunityThreadLifecycleCommandService(
            loadThreadPort,
            saveThreadPort,
            loadMemberPort,
            saveMemberPort,
            getGisuUseCase,
            createChatRoomUseCase,
            inviteManager,
            eventPublisher,
            new CommunityThreadProperties(2),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
        CreateCommunityThreadCommand command = new CreateCommunityThreadCommand(
            OWNER_ID,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            List.of(20L, 30L)
        );

        // when & then
        assertThatThrownBy(() -> sut.create(command))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_CAPACITY_EXCEEDED);
        then(createChatRoomUseCase).shouldHaveNoInteractions();
        then(saveThreadPort).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("수정은 스레드 잠금과 ACTIVE ADMIN 검증 뒤에만 저장한다")
    void update_locksThreadBeforeActorPermission() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember admin = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.ADMIN
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(admin));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(admin));
        given(saveThreadPort.save(thread)).willReturn(thread);
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            THREAD_ID,
            20L,
            "수정 제목",
            null,
            CommunityThreadCategory.STUDY,
            null
        );

        // when
        CommunityThreadLifecycleInfo result = sut.update(command);

        // then
        assertThat(result.title()).isEqualTo("수정 제목");
        assertThat(result.category()).isEqualTo(CommunityThreadCategory.STUDY);
        InOrder order = Mockito.inOrder(loadThreadPort, loadMemberPort, saveThreadPort, eventPublisher);
        order.verify(loadThreadPort).findByIdForUpdate(THREAD_ID);
        order.verify(loadMemberPort).findByThreadIdAndMemberId(THREAD_ID, 20L);
        order.verify(saveThreadPort).save(thread);
        ArgumentCaptor<CommunityThreadUpdatedEvent> eventCaptor = ArgumentCaptor.forClass(
            CommunityThreadUpdatedEvent.class
        );
        order.verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().actorMemberId()).isEqualTo(20L);
    }

    @Test
    @DisplayName("description omitted는 기존 설명을 유지한다")
    void update_omittedDescriptionKeepsExistingValue() {
        // given
        CommunityThread thread = CommunityThread.create(
            CHAT_ROOM_ID,
            "스레드",
            "기존 설명",
            CommunityThreadCategory.FREE,
            "💬",
            OWNER_ID,
            GISU_ID,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", THREAD_ID);
        CommunityThreadMember admin = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.ADMIN
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(admin));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(admin));
        given(saveThreadPort.save(thread)).willReturn(thread);
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            THREAD_ID,
            20L,
            "수정 제목",
            null,
            false,
            null,
            null
        );

        // when
        sut.update(command);

        // then
        assertThat(thread.getDescription()).isEqualTo("기존 설명");
    }

    @Test
    @DisplayName("description blank는 기존 설명을 null로 clear한다")
    void update_blankDescriptionClearsExistingValue() {
        // given
        CommunityThread thread = CommunityThread.create(
            CHAT_ROOM_ID,
            "스레드",
            "기존 설명",
            CommunityThreadCategory.FREE,
            "💬",
            OWNER_ID,
            GISU_ID,
            NOW
        );
        ReflectionTestUtils.setField(thread, "id", THREAD_ID);
        CommunityThreadMember admin = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.ADMIN
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(admin));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(admin));
        given(saveThreadPort.save(thread)).willReturn(thread);
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            THREAD_ID,
            20L,
            null,
            "   ",
            true,
            null,
            null
        );

        // when
        sut.update(command);

        // then
        assertThat(thread.getDescription()).isNull();
    }

    @Test
    @DisplayName("일반 MEMBER는 메타데이터를 수정할 수 없다")
    void update_memberIsDenied() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, 20L)).willReturn(Optional.of(member));
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            THREAD_ID,
            20L,
            "수정 제목",
            null,
            null,
            null
        );

        // when & then
        assertThatThrownBy(() -> sut.update(command))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_ACCESS_DENIED);
        then(saveThreadPort).shouldHaveNoInteractions();
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("soft delete는 활성 대상 스냅샷을 발행하지만 Chat delete 의존성을 갖지 않는다")
    void delete_softDeletesWithoutChatDeleteDependency() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        CommunityThreadMember owner = CommunityThreadLifecycleTestFixtures.activeMember(
            OWNER_ID,
            CommunityThreadMemberRole.OWNER
        );
        CommunityThreadMember member = CommunityThreadLifecycleTestFixtures.activeMember(
            20L,
            CommunityThreadMemberRole.MEMBER
        );
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        given(loadMemberPort.findByThreadIdAndMemberId(THREAD_ID, OWNER_ID)).willReturn(Optional.of(owner));
        given(loadMemberPort.listByThreadId(THREAD_ID)).willReturn(List.of(member, owner));
        given(saveThreadPort.save(thread)).willReturn(thread);

        // when
        CommunityThreadLifecycleInfo result = sut.delete(new ThreadActorCommand(THREAD_ID, OWNER_ID));

        // then
        assertThat(result.deletedAt()).isEqualTo(NOW);
        assertThat(Arrays.stream(CommunityThreadLifecycleCommandService.class.getDeclaredFields())
            .map(Field::getType))
            .doesNotContain(DeleteChatRoomUseCase.class);
        ArgumentCaptor<CommunityThreadDeletedEvent> eventCaptor = ArgumentCaptor.forClass(
            CommunityThreadDeletedEvent.class
        );
        then(eventPublisher).should().publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().activeMemberIds()).containsExactly(10L, 20L);
    }

    @Test
    @DisplayName("삭제된 스레드는 actor 멤버십을 조회하기 전에 거절한다")
    void update_deletedThreadRejectedBeforeActorLookup() {
        // given
        CommunityThread thread = CommunityThreadLifecycleTestFixtures.thread();
        thread.delete(NOW.minusSeconds(1));
        given(loadThreadPort.findByIdForUpdate(THREAD_ID)).willReturn(Optional.of(thread));
        UpdateCommunityThreadCommand command = new UpdateCommunityThreadCommand(
            THREAD_ID,
            OWNER_ID,
            "제목",
            null,
            null,
            null
        );

        // when & then
        assertThatThrownBy(() -> sut.update(command))
            .isInstanceOf(CommunityDomainException.class)
            .extracting(error -> ((CommunityDomainException) error).getBaseCode())
            .isEqualTo(CommunityErrorCode.THREAD_DELETED);
        then(loadMemberPort).shouldHaveNoInteractions();
    }
}
