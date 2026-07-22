package com.umc.product.chat.application.policy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.umc.product.chat.domain.exception.ChatDomainException;
import com.umc.product.chat.domain.exception.ChatErrorCode;

@Component
public class ChatReactionPolicy {

    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    private static final int MAX_CODE_POINTS = 32;

    public void validate(String emoji) {
        if (emoji == null || emoji.isBlank() || emoji.codePointCount(0, emoji.length()) > MAX_CODE_POINTS) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_REACTION);
        }
        Matcher matcher = GRAPHEME.matcher(emoji);
        if (!matcher.matches() || emoji.codePoints().noneMatch(this::isEmojiCodePoint)) {
            throw new ChatDomainException(ChatErrorCode.CHAT_MESSAGE_INVALID_REACTION);
        }
    }

    private boolean isEmojiCodePoint(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.OTHER_SYMBOL
            || type == Character.MODIFIER_SYMBOL
            || codePoint == 0xFE0F
            || codePoint == 0x20E3;
    }
}
