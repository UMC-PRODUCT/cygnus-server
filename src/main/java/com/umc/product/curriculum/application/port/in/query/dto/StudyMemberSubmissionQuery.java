package com.umc.product.curriculum.application.port.in.query.dto;

import java.util.List;

/**
 * 스터디원 제출 현황 조회 조건.
 *
 * @param requesterMemberId 요청 주체 memberId. 조회 가능한 스터디 그룹 범위를 결정한다.
 * @param studyGroupId      특정 스터디 그룹만 조회 (null 이면 권한 범위 내 전체 그룹)
 * @param weekNos           조회할 주차 번호. 비어 있으면 해당 커리큘럼의 전체 주차.
 * @param cursor            직전 페이지 마지막 studyGroupMemberId (첫 페이지는 null)
 * @param size              페이지 크기 (스터디원 기준)
 */
public record StudyMemberSubmissionQuery(
    Long requesterMemberId,
    Long studyGroupId,
    List<Long> weekNos,
    Long cursor,
    int size,
    Long gisuId
) {

    public StudyMemberSubmissionQuery(
        Long requesterMemberId, Long studyGroupId, List<Long> weekNos, Long cursor, int size
    ) {
        this(requesterMemberId, studyGroupId, weekNos, cursor, size, null);
    }

    public StudyMemberSubmissionQuery {
        weekNos = weekNos == null ? List.of() : List.copyOf(weekNos);
    }

    /**
     * hasNext 판별을 위해 한 건 더 조회한다.
     */
    public int fetchSize() {
        return size + 1;
    }
}
