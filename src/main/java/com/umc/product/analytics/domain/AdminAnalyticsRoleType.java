package com.umc.product.analytics.domain;

import com.umc.product.common.domain.enums.ChallengerRoleType;

public enum AdminAnalyticsRoleType {
    SUPER_ADMIN,
    CENTRAL_PRESIDENT,
    CENTRAL_VICE_PRESIDENT,
    CENTRAL_OPERATING_TEAM_MEMBER,
    CENTRAL_EDUCATION_TEAM_MEMBER,
    CHAPTER_PRESIDENT,
    SCHOOL_PRESIDENT,
    SCHOOL_VICE_PRESIDENT,
    SCHOOL_PART_LEADER,
    SCHOOL_ETC_ADMIN;

    public static AdminAnalyticsRoleType from(ChallengerRoleType roleType) {
        return switch (roleType) {
            case CENTRAL_PRESIDENT -> CENTRAL_PRESIDENT;
            case CENTRAL_VICE_PRESIDENT -> CENTRAL_VICE_PRESIDENT;
            case CENTRAL_OPERATING_TEAM_MEMBER -> CENTRAL_OPERATING_TEAM_MEMBER;
            case CENTRAL_EDUCATION_TEAM_MEMBER -> CENTRAL_EDUCATION_TEAM_MEMBER;
            case CHAPTER_PRESIDENT -> CHAPTER_PRESIDENT;
            case SCHOOL_PRESIDENT -> SCHOOL_PRESIDENT;
            case SCHOOL_VICE_PRESIDENT -> SCHOOL_VICE_PRESIDENT;
            case SCHOOL_PART_LEADER -> SCHOOL_PART_LEADER;
            case SCHOOL_ETC_ADMIN -> SCHOOL_ETC_ADMIN;
        };
    }
}
