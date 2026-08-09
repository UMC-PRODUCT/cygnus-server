package com.umc.product.curriculum.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.umc.product.curriculum.domain.enums.ChallengerWorkbookStatus;
import com.umc.product.curriculum.domain.enums.FeedbackResult;
import com.umc.product.curriculum.domain.enums.SubmissionStatus;

/**
 * 미션 피드백으로부터 제출물/워크북 상태를 도출하는 규칙.
 * <p>
 * 워크북 상세 조회와 스터디원 제출 현황 목록이 같은 데이터에 대해 다른 상태를 보여주지 않도록 판정 규칙을 이 한 곳에 모은다. 순수 함수만 두고 조회 방식(단건/배치)에는 관여하지 않는다.
 */
public final class ChallengerWorkbookStatusPolicy {

    private ChallengerWorkbookStatusPolicy() {
    }

    /**
     * 미션 제출물 1건의 상태를 피드백 결과들로부터 도출한다.
     * <p>
     * 한 제출물에 피드백이 여러 건 달릴 수 있고(재검토 등) 결과가 엇갈릴 수 있어, FAIL 을 우선한다. 평가자가 한 번이라도 FAIL 을 남겼다면 통과로 표시하지 않는다.
     *
     * @param feedbackResults 해당 제출물에 달린 피드백 결과들 (없으면 빈 컬렉션)
     * @return 피드백 없음이면 {@code PENDING}, FAIL 이 하나라도 있으면 {@code FAIL}, 그 외에는 {@code PASS}
     */
    public static SubmissionStatus resolveSubmissionStatus(Collection<FeedbackResult> feedbackResults) {
        if (feedbackResults.isEmpty()) {
            return SubmissionStatus.PENDING;
        }
        return feedbackResults.contains(FeedbackResult.FAIL)
            ? SubmissionStatus.FAIL
            : SubmissionStatus.PASS;
    }

    /**
     * 워크북 1건의 상태를 제출물 상태들로부터 집계한다.
     * <p>
     * 인정 처리({@code isExcused})는 제출 여부와 무관하게 PASS 로 본다. 필수 미션이 하나라도 PASS 로 채워지지 않았다면 나머지가 모두 PASS 여도
     * {@code IN_PROGRESS} 이며, 아직 평가가 끝나지 않았다는 뜻이다.
     *
     * @param isExcused                워크북 인정 처리 여부
     * @param submissionStatusByMission 제출물이 있는 미션의 {@code 원본 워크북 미션 ID → 제출물 상태}
     * @param requiredMissionIds       해당 원본 워크북의 필수 미션 ID 집합
     */
    public static ChallengerWorkbookStatus resolveWorkbookStatus(
        boolean isExcused,
        Map<Long, SubmissionStatus> submissionStatusByMission,
        Set<Long> requiredMissionIds
    ) {
        if (isExcused) {
            return ChallengerWorkbookStatus.PASS;
        }
        if (submissionStatusByMission.isEmpty()) {
            return ChallengerWorkbookStatus.IN_PROGRESS;
        }
        if (submissionStatusByMission.containsValue(SubmissionStatus.FAIL)) {
            return ChallengerWorkbookStatus.FAIL;
        }

        boolean allPass = submissionStatusByMission.values().stream()
            .allMatch(status -> status == SubmissionStatus.PASS);
        Set<Long> passedMissionIds = submissionStatusByMission.entrySet().stream()
            .filter(entry -> entry.getValue() == SubmissionStatus.PASS)
            .map(entry -> entry.getKey())
            .collect(Collectors.toSet());

        return allPass && passedMissionIds.containsAll(requiredMissionIds)
            ? ChallengerWorkbookStatus.PASS
            : ChallengerWorkbookStatus.IN_PROGRESS;
    }
}
