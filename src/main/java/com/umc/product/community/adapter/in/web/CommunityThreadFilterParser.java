package com.umc.product.community.adapter.in.web;

import com.umc.product.community.application.port.in.query.thread.dto.ThreadListFilter;

final class CommunityThreadFilterParser {

    private CommunityThreadFilterParser() {
    }

    static ThreadListFilter parse(String rawFilter) {
        return switch (rawFilter) {
            case "all" -> ThreadListFilter.ALL;
            case "unread" -> ThreadListFilter.UNREAD;
            case "STUDY" -> ThreadListFilter.STUDY;
            case "QNA" -> ThreadListFilter.QNA;
            case "PROJECT" -> ThreadListFilter.PROJECT;
            case "FREE" -> ThreadListFilter.FREE;
            default -> throw new IllegalArgumentException("Unsupported thread filter: " + rawFilter);
        };
    }
}
