package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;

import lombok.Builder;

/**
 * 스터디원 1명의 주차별 제출 현황.
 * <p>
 * 화면에서 주차별 카드로 펼쳐 보여주려면 {@code weeks} 를 flatten 하면 된다.
 */
@Builder
public record StudyMemberSubmissionResponse(
    Long studyGroupMemberId,
    Long memberId,
    String memberName,
    String nickname,
    String schoolName,
    String profileImageUrl,
    Long studyGroupId,
    String studyGroupName,
    ChallengerPart part,
    List<WeeklySubmissionResponse> weeks
) {

    public static StudyMemberSubmissionResponse from(StudyMemberSubmissionInfo info) {
        return StudyMemberSubmissionResponse.builder()
            .studyGroupMemberId(info.studyGroupMemberId())
            .memberId(info.memberId())
            .memberName(info.memberName())
            .nickname(info.nickname())
            .schoolName(info.schoolName())
            .profileImageUrl(info.profileImageUrl())
            .studyGroupId(info.studyGroupId())
            .studyGroupName(info.studyGroupName())
            .part(info.part())
            .weeks(info.weeks().stream().map(WeeklySubmissionResponse::from).toList())
            .build();
    }

    /**
     * @param challengerWorkbookId null 이면 워크북 미배포. 상세 조회로 이동할 수 없다.
     * @param isBest               {@code status} 와 독립이다. PASS 이면서 베스트일 수 있다.
     */
    @Builder
    public record WeeklySubmissionResponse(
        Long weekNo,
        Long weeklyCurriculumId,
        String weeklyCurriculumTitle,
        Long challengerWorkbookId,
        ChallengerWorkbookStatus status,
        boolean isBest
    ) {

        private static WeeklySubmissionResponse from(StudyMemberSubmissionInfo.WeeklySubmissionInfo info) {
            return WeeklySubmissionResponse.builder()
                .weekNo(info.weekNo())
                .weeklyCurriculumId(info.weeklyCurriculumId())
                .weeklyCurriculumTitle(info.weeklyCurriculumTitle())
                .challengerWorkbookId(info.challengerWorkbookId())
                .status(info.status())
                .isBest(info.isBest())
                .build();
        }
    }
}
