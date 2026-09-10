package com.umc.product.organization.application.port.in.query.dto.studygroup;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerTrack;

/**
 * 커서 페이지네이션으로 조회한 스터디원 1건. 소속 그룹 정보를 함께 들고 있어 그룹을 다시 조회하지 않아도 된다.
 * <p>
 * 멤버 이름/학교/프로필은 담지 않는다. Member 도메인 소관이므로 Service 가 memberId 로 batch 합성한다 (cross-domain JOIN 회피).
 *
 * @param studyGroupMemberId {@code study_group_member} PK. 커서로 사용된다.
 * @param studyGroupId       소속 스터디 그룹 ID
 * @param studyGroupName     소속 스터디 그룹명
 * @param part               스터디 그룹의 파트 (스터디원 개인 파트가 아니라 그룹의 파트)
 * @param memberId           스터디원 멤버 ID
 */
public record StudyGroupMemberPageInfo(
    Long studyGroupMemberId,
    Long studyGroupId,
    String studyGroupName,
    ChallengerPart part,
    Long memberId,
    ChallengerTrack track
) {
    public StudyGroupMemberPageInfo(
        Long studyGroupMemberId, Long studyGroupId, String studyGroupName,
        ChallengerPart part, Long memberId
    ) {
        this(studyGroupMemberId, studyGroupId, studyGroupName, part, memberId, null);
    }

}
