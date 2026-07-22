package com.umc.product.chat.application.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@DisplayName("ChatReactionPolicy")
class ChatReactionPolicyTest {

    private final ChatReactionPolicy sut = new ChatReactionPolicy();

    @Test
    @DisplayName("ZWJ emoji 한 grapheme cluster를 허용한다")
    void validate_zwjEmoji() {
        assertThatCode(() -> sut.validate("👨‍👩‍👧‍👦")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 grapheme cluster를 거부한다")
    void validate_multipleGraphemes() {
        assertInvalid("👍👍");
    }

    @Test
    @DisplayName("일반 문자를 reaction으로 사용할 수 없다")
    void validate_plainLetter() {
        assertInvalid("A");
    }

    @Test
    @DisplayName("null·blank·길이 초과 reaction을 grapheme 검사 전에 거절한다")
    void validate_requiredBoundaries() {
        assertInvalid(null);
        assertInvalid("   ");
        assertInvalid("👍".repeat(33));
    }

    private void assertInvalid(String emoji) {
        assertThatThrownBy(() -> sut.validate(emoji))
            .isInstanceOf(ChatDomainException.class)
            .extracting(error -> ((ChatDomainException) error).getBaseCode())
            .isEqualTo(ChatErrorCode.CHAT_MESSAGE_INVALID_REACTION);
    }
}
