package com.umc.product.curriculum.adapter.in.web.v2.dto.response;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.curriculum.application.port.in.query.dto.ChallengerWorkbookInfo;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

import lombok.Builder;

@Builder
public record ChallengerWorkbookResponse(
    Long challengerWorkbookId,
    Long originalWorkbookId,
    Long receivedStudyGroupId, // nullable
    Long memberId,
    boolean isExcused,
    String excusedReason,
    String content,
    boolean isBestWorkbook,
    ChallengerWorkbookStatusResponse status,
    boolean hasSubmission,
    MissionSubmissionResponse submission,
    List<MissionSubmissionResponse> submissions
) {
    // 고민거리:
    // 현재 DTO는 내 커리큘럼 조회 시에도 사용됨.
    // 해당 API에서 content가 들어가면 너무 비대해지는 관계로
    // 정팩메로 content 포함 여부를 조절할 수 있도록 하고, 관련 mapper에서 알잘딱깔쎈하는거로

    // ==>
    // 이거 Info 단 DTO를 content 없는거로 두 개 만들어서 QueryDSL에서 Projection으로 해당 DTO 바로 Return 받고,
    // 그거에서 of()로 오는 정팩메 만들어야함

    // status는 OriginalWorkbookStatus와 분리된 것이며, 기존에 존재하던 WorkbookStatus와도 다름.
    // 작성에 주의할 것

    public static ChallengerWorkbookResponse from(ChallengerWorkbookInfo info) {
        List<MissionSubmissionResponse> submissions = info.submissions().stream()
            .map(MissionSubmissionResponse::from)
            .toList();

        return ChallengerWorkbookResponse.builder()
            .challengerWorkbookId(info.challengerWorkbookId())
            .originalWorkbookId(info.originalWorkbookId())
            .receivedStudyGroupId(info.receivedStudyGroupId())
            .memberId(info.challengerId())
            .isExcused(info.isExcused())
            .excusedReason(info.excusedReason())
            .content(info.content())
            .isBestWorkbook(info.isBestWorkbook())
            .status(resolveStatus(info, submissions))
            .hasSubmission(!submissions.isEmpty())
            .submission(submissions.isEmpty() ? null : submissions.get(0))
            .submissions(submissions)
            .build();
    }

    private static ChallengerWorkbookStatusResponse resolveStatus(
        ChallengerWorkbookInfo info,
        List<MissionSubmissionResponse> submissions
    ) {
        if (info.isExcused()) {
            return ChallengerWorkbookStatusResponse.PASS;
        }
        if (submissions.isEmpty()) {
            return ChallengerWorkbookStatusResponse.IN_PROGRESS;
        }
        if (submissions.stream().map(MissionSubmissionResponse::status)
            .anyMatch(status -> status == SubmissionStatus.FAIL)) {
            return ChallengerWorkbookStatusResponse.FAIL;
        }
        boolean allPass = submissions.stream().map(MissionSubmissionResponse::status)
            .allMatch(status -> status == SubmissionStatus.PASS);
        Set<Long> passedMissionIds = submissions.stream()
            .filter(submission -> submission.status() == SubmissionStatus.PASS)
            .map(MissionSubmissionResponse::originalWorkbookMissionId)
            .collect(Collectors.toSet());
        Set<Long> requiredMissionIds =
            info.requiredMissionIds() == null ? Set.of() : info.requiredMissionIds();
        boolean allRequiredMissionsPassed = passedMissionIds.containsAll(requiredMissionIds);
        return allPass && allRequiredMissionsPassed
            ? ChallengerWorkbookStatusResponse.PASS
            : ChallengerWorkbookStatusResponse.IN_PROGRESS;
    }
}
