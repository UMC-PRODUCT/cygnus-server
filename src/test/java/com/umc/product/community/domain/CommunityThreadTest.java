package com.umc.product.community.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.community.domain.enums.CommunityThreadCategory;

@DisplayName("CommunityThread")
class CommunityThreadTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-18T00:00:00Z");

    @Test
    @DisplayName("스레드 생성 시 메타데이터와 활동 시각을 설정한다")
    void create_스레드_메타데이터를_정규화한다() {
        // given & when
        CommunityThread thread = CommunityThread.create(
            10L,
            "  스터디 모집  ",
            "   ",
            CommunityThreadCategory.STUDY,
            " 📚 ",
            20L,
            CREATED_AT
        );

        // then
        assertThat(thread.getChatRoomId()).isEqualTo(10L);
        assertThat(thread.getTitle()).isEqualTo("스터디 모집");
        assertThat(thread.getDescription()).isNull();
        assertThat(thread.getCategory()).isEqualTo(CommunityThreadCategory.STUDY);
        assertThat(thread.getIcon()).isEqualTo("📚");
        assertThat(thread.getCreatorMemberId()).isEqualTo(20L);
        assertThat(thread.getLastActivityAt()).isEqualTo(CREATED_AT);
        assertThat(thread.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("마지막 메시지와 soft delete 시각을 도메인 메서드로 갱신한다")
    void updateProjectionAndDelete_메시지와_soft_delete를_갱신한다() {
        // given
        CommunityThread thread = createThread();
        Instant messageCreatedAt = CREATED_AT.plusSeconds(60);
        Instant deletedAt = CREATED_AT.plusSeconds(120);

        // when
        thread.updateLastMessage(100L, "새 메시지", 21L, messageCreatedAt);
        thread.delete(deletedAt);

        // then
        assertThat(thread.getLastMessageId()).isEqualTo(100L);
        assertThat(thread.getLastMessagePreview()).isEqualTo("새 메시지");
        assertThat(thread.getLastMessageSenderMemberId()).isEqualTo(21L);
        assertThat(thread.getLastMessageCreatedAt()).isEqualTo(messageCreatedAt);
        assertThat(thread.getLastActivityAt()).isEqualTo(messageCreatedAt);
        assertThat(thread.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(thread.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("필수 ID와 메타데이터 경계를 벗어나면 생성을 거절한다")
    void create_유효하지_않은_필드를_거절한다() {
        assertThatThrownBy(() -> CommunityThread.create(
            0L,
            "제목",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            20L,
            CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> CommunityThread.create(
            10L,
            " ",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            20L,
            CREATED_AT
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("마지막 메시지 갱신이 거절되면 기존 projection을 변경하지 않는다")
    void updateLastMessage_잘못된_preview를_거절하면_상태를_유지한다() {
        // given
        CommunityThread thread = createThread();
        Instant previousMessageCreatedAt = CREATED_AT.plusSeconds(60);
        thread.updateLastMessage(100L, "기존 메시지", 21L, previousMessageCreatedAt);
        String tooLongPreview = "가".repeat(2_001);

        // when & then
        assertThatThrownBy(() -> thread.updateLastMessage(
            200L,
            tooLongPreview,
            22L,
            CREATED_AT.plusSeconds(120)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThat(thread.getLastMessageId()).isEqualTo(100L);
        assertThat(thread.getLastMessagePreview()).isEqualTo("기존 메시지");
        assertThat(thread.getLastMessageSenderMemberId()).isEqualTo(21L);
        assertThat(thread.getLastMessageCreatedAt()).isEqualTo(previousMessageCreatedAt);
        assertThat(thread.getLastActivityAt()).isEqualTo(previousMessageCreatedAt);
    }

    @Test
    @DisplayName("마지막 메시지 sender가 없으면 기존 projection을 변경하지 않는다")
    void updateLastMessage_sender를_거절하면_상태를_유지한다() {
        // given
        CommunityThread thread = createThread();
        Instant previousMessageCreatedAt = CREATED_AT.plusSeconds(60);
        thread.updateLastMessage(100L, "기존 메시지", 21L, previousMessageCreatedAt);

        // when & then
        assertThatThrownBy(() -> thread.updateLastMessage(
            200L,
            "새 메시지",
            null,
            CREATED_AT.plusSeconds(120)
        )).isInstanceOf(IllegalArgumentException.class);
        assertThat(thread.getLastMessageId()).isEqualTo(100L);
        assertThat(thread.getLastMessagePreview()).isEqualTo("기존 메시지");
        assertThat(thread.getLastMessageSenderMemberId()).isEqualTo(21L);
        assertThat(thread.getLastMessageCreatedAt()).isEqualTo(previousMessageCreatedAt);
        assertThat(thread.getLastActivityAt()).isEqualTo(previousMessageCreatedAt);
    }

    @Test
    @DisplayName("마지막 메시지 시각이 없으면 기존 projection을 변경하지 않는다")
    void updateLastMessage_시각을_거절하면_상태를_유지한다() {
        // given
        CommunityThread thread = createThread();
        Instant previousMessageCreatedAt = CREATED_AT.plusSeconds(60);
        thread.updateLastMessage(100L, "기존 메시지", 21L, previousMessageCreatedAt);

        // when & then
        assertThatThrownBy(() -> thread.updateLastMessage(200L, "새 메시지", 22L, null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThat(thread.getLastMessageId()).isEqualTo(100L);
        assertThat(thread.getLastMessagePreview()).isEqualTo("기존 메시지");
        assertThat(thread.getLastMessageSenderMemberId()).isEqualTo(21L);
        assertThat(thread.getLastMessageCreatedAt()).isEqualTo(previousMessageCreatedAt);
        assertThat(thread.getLastActivityAt()).isEqualTo(previousMessageCreatedAt);
    }

    private CommunityThread createThread() {
        return CommunityThread.create(
            10L,
            "스레드",
            null,
            CommunityThreadCategory.FREE,
            "💬",
            20L,
            CREATED_AT
        );
    }
}
