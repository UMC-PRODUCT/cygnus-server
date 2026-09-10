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

    /**
     * 제출 현황 화면에서 필터로 선택할 수 있는 주차 번호 목록을 조회한다.
     * <p>
     * 활성 기수의 파트별 커리큘럼에 정의된 weekNo 의 union 이며, {@link #getStudyMemberSubmissions} 결과 행의
     * {@code weeks} 와 동일한 기준(배포 여부 무관)이다. 주차 정보는 공개 데이터(CURRICULUM-101)라 역할 기반 제한을 두지 않는다.
     *
     * @param studyGroupId 특정 그룹의 파트만 볼 때 (null 이면 활성 기수 전체 파트)
     * @return distinct weekNo 오름차순 (커리큘럼이 없으면 빈 리스트)
     * @throws com.umc.product.organization.exception.OrganizationDomainException {@code studyGroupId} 가 존재하지 않을 때
     */
    List<Long> getAvailableWeekNos(Long studyGroupId);

    List<Long> getAvailableWeekNos(Long studyGroupId, Long gisuId);
}
