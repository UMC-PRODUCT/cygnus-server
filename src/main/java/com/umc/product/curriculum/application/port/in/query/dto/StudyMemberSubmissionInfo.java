package com.umc.product.curriculum.application.port.in.query.dto;

import java.util.List;

import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;

import lombok.Builder;

/**
 * 스터디원 1명의 주차별 워크북 제출 현황.
 * <p>
 * 행 단위는 언제나 *스터디원* 이다. 주차는 {@code weeks} 안에 담긴다 — 주차를 행으로 펼치면 필터에 따라 페이지네이션 단위가 사람에서 제출 건으로 바뀌어 커서 의미가 깨지기
 * 때문. 주차를 지정해 조회하면 {@code weeks} 는 1건, 전체 주차면 주차 수만큼이다.
 *
 * @param studyGroupMemberId {@code study_group_member} PK. 커서로 사용된다.
 * @param memberId           스터디원 멤버 ID
 * @param memberName         이름
 * @param nickname           닉네임 (nullable)
 * @param schoolName         학교명 (nullable — 학교 미배정)
 * @param profileImageUrl    프로필 이미지 접근 URL (nullable)
 * @param studyGroupId       소속 스터디 그룹 ID
 * @param studyGroupName     소속 스터디 그룹명
 * @param part               스터디 그룹의 파트
 * @param weeks              주차별 현황 (주차 번호 오름차순)
 */
@Builder
public record StudyMemberSubmissionInfo(
    Long studyGroupMemberId,
    Long memberId,
    String memberName,
    String nickname,
    String schoolName,
    String profileImageUrl,
    Long studyGroupId,
    String studyGroupName,
    ChallengerPart part,
    List<WeeklySubmissionInfo> weeks
) {

    public StudyMemberSubmissionInfo {
        weeks = weeks == null ? List.of() : List.copyOf(weeks);
    }

    /**
     * 특정 주차의 워크북 제출 현황.
     *
     * @param weekNo                 주차 번호
     * @param weeklyCurriculumId     주차 커리큘럼 ID
     * @param weeklyCurriculumTitle  주차 커리큘럼 제목
     * @param challengerWorkbookId   배포된 챌린저 워크북 ID. null 이면 아직 배포되지 않았다는 뜻이며 상세 조회로 이동할 수 없다.
     * @param status                 워크북 상태. 워크북이 없으면 {@code NOT_SUBMITTED}.
     * @param isBest                 해당 그룹·주차의 베스트로 선정되었는지. {@code status} 와 독립이다 (PASS 이면서 베스트일 수 있음).
     */
    @Builder
    public record WeeklySubmissionInfo(
        Long weekNo,
        Long weeklyCurriculumId,
        String weeklyCurriculumTitle,
        Long challengerWorkbookId,
        ChallengerWorkbookStatus status,
        boolean isBest
    ) {
    }
}
