package com.umc.product.project.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.umc.product.project.application.port.out.dto.ProjectMemberMatchedRoundInfo;
import com.umc.product.project.domain.ProjectApplication;
import com.umc.product.project.domain.enums.MatchingType;
import com.umc.product.project.domain.enums.ProjectApplicationStatus;

/**
 * {@code findBy*} — 없어도 정상 ({@link Optional})
 * {@code getBy*} — 반드시 있어야 하며 없으면 {@code ProjectDomainException}
 */
public interface LoadProjectApplicationPort {

    /**
     * 단건 조회. 권한 판정/단건 액션에서 사용합니다.
     */
    Optional<ProjectApplication> findById(Long id);

    boolean existsByAppliedMatchingRoundId(Long matchingRoundId);

    /**
     * (projectId, applicantMemberId, status) 조합으로 본인 지원서를 조회합니다.
     * 멱등 처리(APPLY-001) 및 본인 지원서 식별(APPLY-002/003)에 사용됩니다.
     */
    Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndStatus(
        Long projectId, Long applicantMemberId, ProjectApplicationStatus status
    );

    /**
     * (projectId, applicantMemberId, roundId, status) 조합으로 조회합니다.
     * 현재 오픈된 차수 기준으로 기존 DRAFT를 찾을 때 사용합니다.
     */
    Optional<ProjectApplication> findByProjectIdAndApplicantMemberIdAndRoundIdAndStatus(
        Long projectId, Long applicantMemberId, Long roundId, ProjectApplicationStatus status
    );

    /**
     * (projectId, memberId) 로 단일 DRAFT 지원서를 조회합니다.
     * <p>
     * 같은 프로젝트/멤버에 여러 차수의 DRAFT가 공존할 수 있는 지원서 update/submit 흐름에서는 사용하지 않고,
     * applicationId로 대상을 명시해야 합니다. 없으면 {@code PROJECT_DRAFT_APPLICATION_NOT_FOUND} 예외.
     */
    ProjectApplication getDraftByProjectAndMember(Long projectId, Long memberId);

    /**
     * 동일 차수에 이미 제출된 지원서가 있는지 확인합니다. (중복 제출 방지용)
     */
    boolean existsByRoundAndApplicantAndStatus(
        Long roundId, Long applicantMemberId, ProjectApplicationStatus status
    );

    /**
     * 매칭 차수에 속한 모든 지원서를 조회합니다. 자동 선발 알고리즘 입력으로 사용됩니다.
     */
    List<ProjectApplication> listByMatchingRoundId(Long matchingRoundId);

    /**
     * 같은 매칭 차수와 프로젝트에 속한 결정 가능 지원서(SUBMITTED/APPROVED/REJECTED)를 조회합니다.
     * <p>
     * applicationForm -> project, appliedMatchingRound 를 fetch join 으로 함께 로드하여 최소선발 검증에서 lazy traversal
     * 없이 프로젝트/차수 정보를 사용할 수 있게 합니다.
     */
    List<ProjectApplication> listDecidableByMatchingRoundIdAndProjectId(Long matchingRoundId, Long projectId);

    /**
     * 지원서 단건을 fetch join 으로 조회한다.
     * <p>
     * applicationForm -> project, appliedMatchingRound 를 한 번에 로드하여 호출자가 lazy 프록시 traversal 없이
     * {@code application.getApplicationForm().getProject()} / {@code application.getAppliedMatchingRound()} 를 사용할 수 있게
     * 한다.
     * <p>
     * 미존재는 {@link Optional#empty()} 로 반환한다 — 정합성(요청 projectId 와 application 의 form.project.id 일치 여부) 검증은 호출자(Service)의
     * 책임.
     */
    Optional<ProjectApplication> findByIdWithDetails(Long applicationId);

    /**
     * 여러 지원서를 fetch join 으로 조회한다.
     * <p>
     * applicationForm -> project, appliedMatchingRound 를 한 번에 로드한다. 입력된 모든 ID가 존재해야 하며, 누락 시 예외를 던진다.
     */
    List<ProjectApplication> batchGetByIdsWithDetails(Collection<Long> applicationIds);

    /**
     * 본인 지원 내역을 조회한다.
     * <p>
     * applicationForm/project/matchingRound 를 fetch join 으로 함께 로드하므로 호출자는
     * {@code application.getApplicationForm().getProject()} 등을 추가 쿼리 없이 사용할 수 있다.
     * <p>
     * 정렬: 매칭 라운드 시작일 ASC -> 지원서 갱신일 DESC.
     *
     * @param applicantMemberId 지원자 Member ID
     * @param gisuId            기수 ID (해당 기수 프로젝트만 조회)
     * @param matchingType      매칭 종류 (사용자 파트 기준 자동 결정)
     * @param status            상태 필터. {@code null} 이면 DRAFT 제외 전체.
     */
    List<ProjectApplication> searchMyApplications(
        Long applicantMemberId,
        Long gisuId,
        MatchingType matchingType,
        ProjectApplicationStatus status
    );

    /**
     * PM/운영진용 단일 프로젝트의 지원자 목록을 조회한다.
     * <p>
     * appliedMatchingRound 를 fetch join 으로 함께 로드한다. 임시저장(DRAFT)은 결과에서 제외된다.
     * <p>
     * 정렬: matchingRound.phase ASC -> submittedAt ASC.
     * <p>
     * 파트 필터는 challenger 도메인 속성이라 본 port 에서 다루지 않는다 -- 호출자(Service) 가 challenger 정보를 enrich 한 뒤 in-memory 로 필터링한다.
     *
     * 기본적으로 지원(모집)이 끝난 차수({@code endsAt < now})의 지원서만 반환한다. matchingRoundId 미지정(전체 조회) 시에도 진행 중인 차수의
     * 지원서는 자동으로 제외된다. 단, {@code includeOngoingMatchingRounds} 가 true 면 진행 중인 차수의 지원서도 함께 반환한다.
     *
     * @param projectId       대상 프로젝트 ID
     * @param matchingRoundId 매칭 차수 필터 (선택). null 이면 전체.
     * @param status          상태 필터 (선택). null 이면 DRAFT 제외 전체.
     * @param now                           조회 기준 시각. 이 시각보다 endsAt 이 이른 차수만 노출된다.
     * @param includeOngoingMatchingRounds 진행 중인 매칭 차수 포함 여부
     */
    List<ProjectApplication> searchProjectApplications(
        Long projectId,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now,
        boolean includeOngoingMatchingRounds
    );

    /**
     * PM/운영진용 복수 프로젝트의 지원자 목록을 조회한다.
     * <p>
     * 임시저장(DRAFT)은 결과에서 제외된다. {@code includeOngoingProjectIds} 에 포함된 프로젝트는 진행 중인 차수의 지원서도 반환하고, 그 외 프로젝트는
     * 지원(모집)이 끝난 차수({@code endsAt < now})의 지원서만 반환한다.
     *
     * @param projectIds                대상 프로젝트 ID 목록
     * @param includeOngoingProjectIds  진행 중인 매칭 차수까지 노출할 프로젝트 ID 목록
     * @param matchingRoundId           매칭 차수 필터 (선택). null 이면 전체.
     * @param status                    상태 필터 (선택). null 이면 DRAFT 제외 전체.
     * @param now                       조회 기준 시각
     */
    List<ProjectApplication> searchProjectApplicationsByProjectIds(
        Collection<Long> projectIds,
        Collection<Long> includeOngoingProjectIds,
        Long matchingRoundId,
        ProjectApplicationStatus status,
        Instant now
    );

    /**
     * 프로젝트/멤버 쌍별 APPROVED 지원서 중 가장 최신 매칭 차수를 조회합니다.
     * <p>
     * 최신 기준: matchingRound.startsAt DESC, 동률이면 application.id DESC.
     * 지원서가 없는 쌍은 결과에 포함하지 않습니다.
     */
    List<ProjectMemberMatchedRoundInfo> listLatestApprovedMatchedRoundsByProjectIdsAndMemberIds(
        Collection<Long> projectIds,
        Collection<Long> memberIds
    );

    /**
     * 프로젝트 abort 시 일괄 취소 대상 application 조회용. DRAFT/SUBMITTED 상태 application 만 반환합니다.
     * APPROVED/REJECTED/CANCELLED 는 이미 종결되어 추가 정리가 필요 없습니다.
     */
    List<ProjectApplication> listInProgressByProjectId(Long projectId);
}
