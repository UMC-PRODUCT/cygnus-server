package com.umc.product.chat.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.chat.adapter.out.persistence.ChatRoomPersistenceAdapter;
import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.query.dto.ChatRoomInfo;
import com.umc.product.chat.application.port.out.SaveChatMemberPort;
import com.umc.product.chat.domain.ChatMember;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.support.IntegrationTestSupport;

@Import(ChatRoomCommandServiceAtomicityTest.FailureInjectionConfig.class)
@DisplayName("ChatRoomCommandService standalone 생성 원자성")
class ChatRoomCommandServiceAtomicityTest extends IntegrationTestSupport {

    @Autowired
    CreateChatRoomUseCase createChatRoomUseCase;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    FailingSaveChatMemberPort failingSaveChatMemberPort;

    @BeforeEach
    void resetFailure() {
        failingSaveChatMemberPort.disarm();
    }

    @Test
    @DisplayName("standalone 생성은 room ID를 owner key로 binding하고 creator membership을 함께 저장한다")
    void create_savesRoomOwnershipAndCreatorMembership() {
        ChatRoomInfo result = createChatRoomUseCase.create(
            new CreateChatRoomCommand(ChatRoomActorContext.actor(101L)));

        assertThat(count("chat_room")).isOne();
        assertThat(count("chat_room_ownership")).isOne();
        assertThat(count("chat_member")).isOne();
        assertThat(jdbcTemplate.queryForObject(
            "select namespace from chat_room_ownership where room_id = ?", String.class, result.roomId()))
            .isEqualTo(ChatRoomOwnerReference.STANDALONE_NAMESPACE);
        assertThat(jdbcTemplate.queryForObject(
            "select owner_resource_key from chat_room_ownership where room_id = ?", String.class, result.roomId()))
            .isEqualTo(result.roomId().toString());
        assertThat(jdbcTemplate.queryForObject(
            "select slot from chat_room_ownership where room_id = ?", String.class, result.roomId()))
            .isEqualTo(ChatRoomOwnerReference.DEFAULT_SLOT);
        assertThat(jdbcTemplate.queryForObject(
            "select member_id from chat_member where room_id = ?", Long.class, result.roomId()))
            .isEqualTo(101L);
    }

    @Test
    @DisplayName("creator membership 저장 실패 시 room과 ownership도 같은 transaction에서 rollback한다")
    void create_membershipFailure_rollsBackAllRows() {
        failingSaveChatMemberPort.arm();

        assertThatThrownBy(() -> createChatRoomUseCase.create(
            new CreateChatRoomCommand(ChatRoomActorContext.actor(101L))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("injected membership failure");

        assertThat(count("chat_room")).isZero();
        assertThat(count("chat_room_ownership")).isZero();
        assertThat(count("chat_member")).isZero();
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    @TestConfiguration
    static class FailureInjectionConfig {

        @Bean
        @Primary
        FailingSaveChatMemberPort failingSaveChatMemberPort(ChatRoomPersistenceAdapter delegate) {
            return new FailingSaveChatMemberPort(delegate);
        }
    }

    static final class FailingSaveChatMemberPort implements SaveChatMemberPort {

        private final ChatRoomPersistenceAdapter delegate;
        private boolean armed;

        FailingSaveChatMemberPort(ChatRoomPersistenceAdapter delegate) {
            this.delegate = delegate;
        }

        void arm() {
            armed = true;
        }

        void disarm() {
            armed = false;
        }

        @Override
        public ChatMember save(ChatMember chatMember) {
            ChatMember saved = delegate.save(chatMember);
            if (armed) {
                throw new IllegalStateException("injected membership failure");
            }
            return saved;
        }

        @Override
        public boolean saveIfAbsent(ChatMember chatMember) {
            return delegate.saveIfAbsent(chatMember);
        }

        @Override
        public void delete(Long roomId, Long memberId) {
            delegate.delete(roomId, memberId);
        }

        @Override
        public void bumpLastReadMessageId(Long roomId, Long memberId, long candidateMessageId) {
            delegate.bumpLastReadMessageId(roomId, memberId, candidateMessageId);
        }
    }

}
