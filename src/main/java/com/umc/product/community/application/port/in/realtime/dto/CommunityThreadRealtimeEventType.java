package com.umc.product.community.application.port.in.realtime.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CommunityThreadRealtimeEventType {
    COMMAND_ACKNOWLEDGED("command.acknowledged"),
    MESSAGE_CREATED("message.created"),
    MESSAGE_UPDATED("message.updated"),
    MESSAGE_DELETED("message.deleted"),
    REACTION_CHANGED("reaction.changed"),
    READ_UPDATED("read.updated"),
    THREAD_INVITED("thread.invited"),
    THREAD_UPDATED("thread.updated"),
    THREAD_DELETED("thread.deleted"),
    MEMBER_KICKED("member.kicked"),
    MEMBER_LEFT("member.left");

    private final String value;

    CommunityThreadRealtimeEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
