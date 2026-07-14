package com.umc.product.project.application.port.in.query;

import java.util.Collection;

import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectMatchingStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ChapterProjectStatisticsInfo;
import com.umc.product.project.application.port.in.query.dto.statistics.ProjectStatisticsInfo;

/**
 * 프로젝트 지원/매칭 현황 통합 조회 UseCase.
 */
public interface GetProjectStatisticsUseCase {

    /**
     * 단건 프로젝트의 활성 멤버와 해당 멤버들이 작성한 지원 이력을 조회합니다.
     * <p>
     * 접근 권한: 해당 프로젝트의 PO/Sub-PM(본인 프로젝트), 총괄단, 해당 지부장, 해당 지부 소속 학교 회장·부회장만
     * 조회할 수 있다. 그 외는 {@code PROJECT_ACCESS_DENIED}.
     */
    ProjectStatisticsInfo getByProjectId(Long projectId, Long requesterMemberId);

    /**
     * 지부 내 전체 프로젝트의 활성 멤버와 BFF 요약 통계를 조회합니다.
     * <p>
     * 접근 권한: 총괄단 / 해당 지부장 / 해당 지부 소속 학교 회장·부회장만 조회할 수 있다.
     * 그 외는 {@code PROJECT_ACCESS_DENIED}. (PO/Sub-PM 은 본인 프로젝트만 단건 조회로 본다.)
     */
    ChapterProjectStatisticsInfo getByChapterId(Long chapterId, Long requesterMemberId);

    /**
     * 지정한 프로젝트들의 활성 멤버와 BFF 요약 통계를 조회합니다.
     * <p>
     * 모든 프로젝트는 같은 지부에 속해야 하며, 요청자는 각 프로젝트에 대한 단건 통계 조회 권한이 있어야 한다.
     */
    ChapterProjectStatisticsInfo getByProjectIds(Collection<Long> projectIds, Long requesterMemberId);

    /**
     * 로그인 사용자에게 공개할 지부 내 프로젝트 매칭 요약 통계를 조회합니다.
     * <p>
     * 멤버/지원서 식별자는 노출하지 않고 ProjectMember 기준 집계 숫자만 반환한다.
     */
    ChapterProjectMatchingStatisticsInfo getPublicMatchingStatisticsByChapterId(
        Long chapterId,
        Long requesterMemberId
    );
}
