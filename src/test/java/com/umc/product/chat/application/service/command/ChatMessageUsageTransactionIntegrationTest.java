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

import com.umc.product.chat.application.port.in.command.CreateChatRoomUseCase;
import com.umc.product.chat.application.port.in.command.SendChatMessageUseCase;
import com.umc.product.chat.application.port.in.command.dto.CreateChatRoomCommand;
import com.umc.product.chat.application.port.in.command.dto.SendChatMessageCommand;
import com.umc.product.chat.domain.ChatRoomActorContext;
import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.chat.domain.MessageContentType;
import com.umc.product.storage.application.port.in.command.ManageFileUsageUseCase;
import com.umc.product.storage.application.port.in.command.dto.BulkRemoveFileUsagesCommand;
import com.umc.product.storage.application.port.in.command.dto.ReplaceFileUsagesCommand;
import com.umc.product.support.IntegrationTestSupport;

@Import(ChatMessageUsageTransactionIntegrationTest.FailureConfig.class)
@DisplayName("ChatMessage와 usage transaction")
class ChatMessageUsageTransactionIntegrationTest extends IntegrationTestSupport {

    @Autowired
    CreateChatRoomUseCase createChatRoomUseCase;
    @Autowired
    SendChatMessageUseCase sendChatMessageUseCase;
    @Autowired
    FailingManageFileUsageUseCase failingManageFileUsageUseCase;
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void disarmFailure() {
        failingManageFileUsageUseCase.disarm();
    }

    @Test
    @DisplayName("message 저장 후 usage 실패는 message와 watermark를 rollback한다")
    void usageFailureRollsBackMessageAndWatermark() {
        long roomId = createChatRoomUseCase.create(
            new CreateChatRoomCommand(ChatRoomActorContext.actor(10L))).roomId();
        failingManageFileUsageUseCase.arm();

        assertThatThrownBy(() -> sendChatMessageUseCase.send(new SendChatMessageCommand(
            ChatRoomOwnerReference.standalone(roomId),
            ChatRoomActorContext.actor(10L),
            MessageContentType.TEXT,
            "rollback",
            null
        )))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("injected usage failure");

        assertThat(count("chat_message")).isZero();
        assertThat(jdbcTemplate.queryForObject(
            "select last_read_message_id from chat_member where room_id = ? and member_id = 10",
            Long.class,
            roomId
        )).isNull();
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }

    @TestConfiguration
    static class FailureConfig {

        @Bean
        @Primary
        FailingManageFileUsageUseCase failingManageFileUsageUseCase() {
            return new FailingManageFileUsageUseCase();
        }
    }

    static final class FailingManageFileUsageUseCase implements ManageFileUsageUseCase {

        private boolean armed;

        void arm() {
            armed = true;
        }

        void disarm() {
            armed = false;
        }

        @Override
        public void replaceUsages(ReplaceFileUsagesCommand command) {
            if (armed) {
                throw new IllegalStateException("injected usage failure");
            }
        }

        @Override
        public void removeAll(BulkRemoveFileUsagesCommand command) {
        }
    }
}
