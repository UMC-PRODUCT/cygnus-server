package com.umc.product.chat.application.policy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.umc.product.chat.application.port.in.command.dto.CreateChatMessageCommand;

public final class ChatMessagePayloadFingerprint {

    private ChatMessagePayloadFingerprint() {
    }

    public static String from(CreateChatMessageCommand command) {
        MessageDigest digest = sha256();
        appendString(digest, command.contentType().name());
        appendNullableString(digest, command.content());
        appendStrings(digest, command.fileMetadataIds());
        appendNullableLong(digest, command.replyToMessageId());
        appendLongs(digest, command.mentionedMemberIds());
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void appendNullableString(MessageDigest digest, String value) {
        if (value == null) {
            appendInt(digest, -1);
            return;
        }
        appendString(digest, value);
    }

    private static void appendString(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        appendInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void appendStrings(MessageDigest digest, List<String> values) {
        appendInt(digest, values.size());
        values.forEach(value -> appendString(digest, value));
    }

    private static void appendNullableLong(MessageDigest digest, Long value) {
        digest.update((byte) (value == null ? 0 : 1));
        if (value != null) {
            digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array());
        }
    }

    private static void appendLongs(MessageDigest digest, List<Long> values) {
        appendInt(digest, values.size());
        values.forEach(value -> digest.update(ByteBuffer.allocate(Long.BYTES).putLong(value).array()));
    }

    private static void appendInt(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }
}
