package com.umc.product.curriculum.application.port.in.query;

import java.util.List;

import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionInfo;
import com.umc.product.curriculum.application.port.in.query.dto.StudyMemberSubmissionQuery;

/**
 * 스터디원 제출 현황 조회 UseCase (활동 관리 &gt; 스터디 관리 &gt; 제출 현황).
 */
public interface GetStudyMemberSubmissionUseCase {

    /**
     * 요청자가 관리할 수 있는 스터디 그룹의 스터디원들과, 각자의 주차별 워크북 제출 현황을 조회한다.
     * <p>
     * 모수는 제출물이 아니라 {@code study_group_member} 이므로 아직 제출하지 않은 인원도 결과에 포함된다.
     *
     * @return 스터디원 목록 (권한 범위가 비면 빈 리스트). hasNext 판별을 위해 {@code size + 1} 건까지 반환될 수 있다.
     */
    List<StudyMemberSubmissionInfo> getStudyMemberSubmissions(StudyMemberSubmissionQuery query);
}
