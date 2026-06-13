package com.umc.product.chat.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.product.chat.application.port.out.dto.RoomUnreadCount;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatMessage;
import com.umc.product.chat.domain.ChatRoom;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.global.config.JpaConfig;
import com.umc.product.global.config.QueryDslConfig;
import com.umc.product.support.TestContainersConfig;
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
    @DisplayName("findLatestMessageId: 방의 가장 최신 메시지 id를 반환한다")
    void findLatestMessageId() {
        persistText(roomId, OTHER, "1");
        Long last = persistText(roomId, OTHER, "2");
        flushAndClear();

        assertThat(sut.findLatestMessageId(roomId)).contains(last);
    }

    @Test
    @DisplayName("findLatestMessageId: 메시지가 없는 방이면 Optional.empty를 반환한다")
    void findLatestMessageId_emptyRoom() {
        Long emptyRoomId = em.persist(ChatRoom.create()).getId();
        flushAndClear();

        assertThat(sut.findLatestMessageId(emptyRoomId)).isEmpty();
    }

    @Test
    @DisplayName("countUnreadByRooms: lastRead 초과분만 세고 본인 메시지는 제외, 시스템 메시지는 포함한다")
    void countUnreadByRooms() {
        Long readUpTo = persistText(roomId, OTHER, "읽은 메시지");   // lastRead 기준
        persistText(roomId, ME, "내가 보낸 메시지");                  // 제외 (본인)
        persistText(roomId, OTHER, "안 읽은 상대 메시지");           // 카운트
        persistSystem(roomId, "시스템 메시지");                       // 카운트 (sender null)
        persistMember(roomId, ME, readUpTo);
        flushAndClear();

        List<RoomUnreadCount> result = sut.countUnreadByRooms(ME, List.of(roomId));

        assertThat(result).containsExactly(new RoomUnreadCount(roomId, 2L));
    }

    @Test
    @DisplayName("countUnreadByRooms: lastRead가 null이면 본인 외 모든 메시지를 센다")
    void countUnreadByRooms_nullLastRead() {
        persistText(roomId, OTHER, "상대 메시지");
        persistText(roomId, ME, "내 메시지");
        persistMember(roomId, ME, null);
        flushAndClear();

        List<RoomUnreadCount> result = sut.countUnreadByRooms(ME, List.of(roomId));

        assertThat(result).containsExactly(new RoomUnreadCount(roomId, 1L));
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
