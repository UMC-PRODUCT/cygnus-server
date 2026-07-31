package com.umc.product.community.domain;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.community.thread")
public record CommunityThreadProperties(Integer maxMembers) {

    public static final int DEFAULT_MAX_MEMBERS = 100;
    public static final int MIN_MAX_MEMBERS = 2;
    public static final int MAX_MAX_MEMBERS = 100;

    public CommunityThreadProperties {
        maxMembers = maxMembers == null ? DEFAULT_MAX_MEMBERS : maxMembers;
        if (maxMembers < MIN_MAX_MEMBERS || maxMembers > MAX_MAX_MEMBERS) {
            throw new IllegalArgumentException("maxMembers must be between 2 and 100");
        }
    }

    public boolean allowsCreateInviteCount(int inviteCount) {
        return inviteCount >= 0 && inviteCount < maxMembers;
    }

    public boolean allowsInvite(int activeMemberCount, int inviteCount) {
        return activeMemberCount >= 0
            && inviteCount > 0
            && activeMemberCount <= maxMembers
            && inviteCount <= maxMembers - activeMemberCount;
    }

    public boolean allowsFanOutRecipientCount(int recipientCount) {
        return recipientCount >= 0 && recipientCount <= maxMembers;
    }

    public int remainingCapacity(int activeMemberCount) {
        if (activeMemberCount < 0) {
            throw new IllegalArgumentException("activeMemberCount must not be negative");
        }
        return Math.max(0, maxMembers - activeMemberCount);
    }
}
