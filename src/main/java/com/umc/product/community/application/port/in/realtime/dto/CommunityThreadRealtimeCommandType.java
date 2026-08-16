package com.umc.product.community.application.port.in.realtime.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CommunityThreadRealtimeCommandType {
    MESSAGE_CREATE("MESSAGE_CREATE"),
    MESSAGE_EDIT("MESSAGE_EDIT"),
    MESSAGE_DELETE("MESSAGE_DELETE"),
    REACTION_ADD("REACTION_ADD"),
    REACTION_REMOVE("REACTION_REMOVE"),
    READ_UPDATE("READ_UPDATE");

    private final String value;

    CommunityThreadRealtimeCommandType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
