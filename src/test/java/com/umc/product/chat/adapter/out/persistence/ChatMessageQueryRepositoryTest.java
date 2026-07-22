package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaConfig.class, QueryDslConfig.class, TestContainersConfig.class, ChatMessageQueryRepository.class})
@DisplayName("ChatMessageQueryRepository")
class ChatMessageQueryRepositoryTest {

    private static final Long ME = 10L;
    private static final Long OTHER = 20L;

    @Autowired
    TestEntityManager em;

    @Autowired
    ChatMessageQueryRepository sut;

    @Autowired
    ChatMessageJpaRepository chatMessageJpaRepository;

    private Long roomId;

    @BeforeEach
    void setUp() {
        roomId = em.persist(ChatRoom.create()).getId();
    }

    @Test
    @DisplayName("listByRoomId: 최신순(id desc)으로 limit만큼 조회한다")
    void listByRoomId_latestFirst() {
        persistText(roomId, OTHER, "1");
        persistText(roomId, OTHER, "2");
        Long third = persistText(roomId, OTHER, "3");
        Long fourth = persistText(roomId, OTHER, "4");
        Long fifth = persistText(roomId, OTHER, "5");
        flushAndClear();

        List<ChatMessage> result = sut.listByRoomId(roomId, null, 3);

        assertThat(result).extracting(ChatMessage::getId).containsExactly(fifth, fourth, third);
    }

    @Test
    @DisplayName("listByRoomId: 커서(id 미만)부터 더 과거 메시지를 조회한다")
    void listByRoomId_withCursor() {
        Long first = persistText(roomId, OTHER, "1");
        Long second = persistText(roomId, OTHER, "2");
        Long third = persistText(roomId, OTHER, "3");
        flushAndClear();

        List<ChatMessage> result = sut.listByRoomId(roomId, third, 10);

        assertThat(result).extracting(ChatMessage::getId).containsExactly(second, first);
    }

    @Test
    @DisplayName("listLatestPerRoom: 방마다 마지막(최신) 메시지 한 건씩만 조회한다")
    void listLatestPerRoom() {
        Long otherRoomId = em.persist(ChatRoom.create()).getId();
        persistText(roomId, OTHER, "old");
        Long roomLast = persistText(roomId, OTHER, "new");
        Long otherLast = persistText(otherRoomId, OTHER, "only");
        // 메시지가 없는 방
        Long emptyRoomId = em.persist(ChatRoom.create()).getId();
        flushAndClear();

        List<ChatMessage> result = sut.listLatestPerRoom(List.of(roomId, otherRoomId, emptyRoomId));

        Map<Long, Long> latestIdByRoom = result.stream()
            .collect(Collectors.toMap(ChatMessage::getRoomId, ChatMessage::getId));
        assertThat(latestIdByRoom).containsOnly(
            Map.entry(roomId, roomLast),
            Map.entry(otherRoomId, otherLast)
        );
    }

    @Test
    @DisplayName("빈 방 ID 입력과 메시지가 없는 방 목록은 빈 결과를 반환한다")
    void empty_room_inputs() {
        Long emptyRoomId = em.persist(ChatRoom.create()).getId();
        flushAndClear();

        assertThat(sut.listLatestPerRoom(null)).isEmpty();
        assertThat(sut.listLatestPerRoom(List.of())).isEmpty();
        assertThat(sut.listLatestPerRoom(List.of(emptyRoomId))).isEmpty();
        assertThat(sut.countUnreadByRooms(ME, null)).isEmpty();
        assertThat(sut.countUnreadByRooms(ME, List.of())).isEmpty();
    }

    @Test
    @DisplayName("replyToMessageId를 저장하고 조회할 수 있다")
    void replyToMessageId_persist() {
        Long originalMessageId = persistText(roomId, OTHER, "원본 메시지");
        Long replyMessageId = em.persist(
            ChatMessage.create(roomId, ME, MessageContentType.FILE, "답장 메시지", List.of("file-1"), originalMessageId)
        ).getId();
        flushAndClear();

        ChatMessage reply = em.find(ChatMessage.class, replyMessageId);

        assertThat(reply.getReplyToMessageId()).isEqualTo(originalMessageId);
    }

    @Test
    @DisplayName("existsByIdAndRoomId: 메시지가 해당 방에 있을 때만 true를 반환한다")
    void existsByIdAndRoomId() {
        Long messageId = persistText(roomId, OTHER, "방 1 메시지");
        Long otherRoomId = em.persist(ChatRoom.create()).getId();
        flushAndClear();

        assertThat(chatMessageJpaRepository.existsByIdAndRoomId(messageId, roomId)).isTrue();
        assertThat(chatMessageJpaRepository.existsByIdAndRoomId(messageId, otherRoomId)).isFalse();
    }

    @Test
    @DisplayName("findByIdAndRoomId: 메시지가 해당 방에 있을 때만 반환한다")
    void findByIdAndRoomId() {
        Long messageId = persistText(roomId, OTHER, "방 1 메시지");
        Long otherRoomId = em.persist(ChatRoom.create()).getId();
        flushAndClear();

        assertThat(chatMessageJpaRepository.findByIdAndRoomId(messageId, roomId)).isPresent();
        assertThat(chatMessageJpaRepository.findByIdAndRoomId(messageId, otherRoomId)).isEmpty();
    }

    @Test
    @DisplayName("countUnreadByRooms: lastRead 초과분은 본인 메시지와 시스템 메시지를 포함해 센다")
    void countUnreadByRooms() {
        Long readUpTo = persistText(roomId, OTHER, "읽은 메시지");
        persistText(roomId, ME, "내가 보낸 메시지");
        persistText(roomId, OTHER, "안 읽은 상대 메시지");
        persistSystem(roomId, "시스템 메시지");
        persistMember(roomId, ME, readUpTo);
        flushAndClear();

        List<RoomUnreadCount> result = sut.countUnreadByRooms(ME, List.of(roomId));

        assertThat(result).containsExactly(new RoomUnreadCount(roomId, 3L));
    }

    @Test
    @DisplayName("countUnreadByRooms: lastRead가 null이면 본인 메시지도 포함해 모든 메시지를 센다")
    void countUnreadByRooms_nullLastRead() {
        persistText(roomId, OTHER, "상대 메시지");
        persistText(roomId, ME, "내 메시지");
        persistMember(roomId, ME, null);
        flushAndClear();

        List<RoomUnreadCount> result = sut.countUnreadByRooms(ME, List.of(roomId));

        assertThat(result).containsExactly(new RoomUnreadCount(roomId, 2L));
    }

    private Long persistText(Long roomId, Long senderMemberId, String content) {
        return em.persist(ChatMessage.create(roomId, senderMemberId, MessageContentType.TEXT, content, null)).getId();
    }

    private Long persistSystem(Long roomId, String content) {
        return em.persist(ChatMessage.createSystem(roomId, content)).getId();
    }

    private void persistMember(Long roomId, Long memberId, Long lastReadMessageId) {
        ChatMember member = ChatMember.of(roomId, memberId);
        member.markRead(lastReadMessageId);
        em.persist(member);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
