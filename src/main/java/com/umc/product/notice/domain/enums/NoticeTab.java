package com.umc.product.notice.domain.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.umc.product.common.domain.enums.ChallengerRoleType;

import lombok.Getter;

/**
 * 공지 대상 역할 유형. CHALLENGER는 일반 챌린저 공지, 나머지는 운영진 공지의 하한선 역할을 나타냅니다. level이 낮을수록 상위 직급 (CENTRAL_MEMBER=1 > SCHOOL_CORE=2 >
 * SCHOOL_PART_LEADER=3).
 */
@Getter
public enum NoticeTab {

    CHALLENGER(0),
    CENTRAL_MEMBER(1),  // 교육국 + 운영국 통합
    SCHOOL_CORE(2),     // 교내 회장단
    SCHOOL_PART_LEADER(3);

    private final int level;

    NoticeTab(int level) {
        this.level = level;
    }

    /**
     * viewerRole이 읽을 수 있는 운영진 공지의 targetNoticeTab 목록을 반환합니다.
     */
    public static List<NoticeTab> staffRolesReadableBy(NoticeTab viewerRole) {
        if (viewerRole == null || viewerRole == CHALLENGER) {
            return List.of();
        }
        return Arrays.stream(values())
            .filter(r -> r != CHALLENGER && viewerRole.level <= r.level)
            .toList();
    }

    /**
     * ChallengerRoleType을 공지 대상 역할로 변환합니다. 총괄단/지부장은 최상단에서 별도 처리되므로 empty를 반환합니다.
     */
    public static Optional<NoticeTab> findFrom(ChallengerRoleType roleType) {
        return Optional.ofNullable(switch (roleType) {
            case SCHOOL_PART_LEADER, SCHOOL_ETC_ADMIN -> SCHOOL_PART_LEADER;
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT, CHAPTER_PRESIDENT -> SCHOOL_CORE;
            case CENTRAL_EDUCATION_TEAM_MEMBER, CENTRAL_OPERATING_TEAM_MEMBER -> CENTRAL_MEMBER;
            case CENTRAL_PRESIDENT, CENTRAL_VICE_PRESIDENT -> null;
        });
    }

    public boolean isStaffRole() {
        return this != CHALLENGER;
    }

    /**
     * 이 공지(targetNoticeTab)를 viewerRole 보유자가 읽을 수 있는지 확인합니다. viewerRole의 level이 이 역할의 level 이하이면 읽기 가능합니다.
     */
    public boolean includes(NoticeTab viewerRole) {
        if (viewerRole == null || viewerRole == CHALLENGER) {
            return false;
        }
        return viewerRole.level <= this.level;
    }
}
