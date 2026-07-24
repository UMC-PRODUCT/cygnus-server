package com.umc.product.chat.application.authorization;

public enum ChatPolicyAction {
    ROOM_READ("chat-room:read"),
    ROOM_SUMMARY_READ("chat-room-summary:read"),
    MESSAGE_CREATE("chat-message:create"),
    MESSAGE_READ("chat-message:read"),
    MESSAGE_UPDATE("chat-message:update"),
    MESSAGE_DELETE("chat-message:delete"),
    REACTION_UPDATE("chat-reaction:update"),
    READ_UPDATE("chat-read:update"),
    READ_STATUS("chat-read-status:read");

    private final String id;

    ChatPolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
