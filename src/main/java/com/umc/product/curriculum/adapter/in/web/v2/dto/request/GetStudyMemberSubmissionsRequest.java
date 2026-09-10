package com.umc.product.curriculum.adapter.in.web.v2.dto.request;

import java.util.List;

import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionQuery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 스터디원 제출 현황 조회 요청.
 *
 * @param studyGroupId 특정 스터디 그룹만 조회. 생략하면 요청자가 관리할 수 있는 전체 그룹.
 * @param weekNos      조회할 주차. 생략하면 전체 주차.
 * @param cursor       직전 페이지 마지막 studyGroupMemberId
 * @param size         페이지 크기 (스터디원 기준). 기본 20.
 */
public record GetStudyMemberSubmissionsRequest(
    @Positive Long studyGroupId,
    List<@PositiveOrZero Long> weekNos,
    @Positive Long cursor,
    @Positive @Max(100) Integer size,
    @Positive Long gisuId
) {
    private static final int DEFAULT_SIZE = 20;

    public GetStudyMemberSubmissionsRequest(Long studyGroupId, List<Long> weekNos, Long cursor, Integer size) {
        this(studyGroupId, weekNos, cursor, size, null);
    }

    public StudyMemberSubmissionQuery toQuery(Long requesterMemberId) {
        return new StudyMemberSubmissionQuery(
            requesterMemberId,
            studyGroupId,
            weekNos == null ? List.of() : weekNos,
            cursor,
            resolvedSize(),
            gisuId
        );
    }

    /**
     * 요청 크기. hasNext 판별을 위해 Service 는 한 건 더 조회하므로, 응답을 자를 때 이 값을 기준으로 삼는다.
     */
    public int resolvedSize() {
        return size == null ? DEFAULT_SIZE : size;
    }
}
