package com.umc.product.curriculum.domain.enums;

/**
 * 챌린저 워크북 한 건의 평가 상태.
 * <p>
 * 미션 제출물 단위 상태인 {@link SubmissionStatus} 를 워크북 단위로 집계한 결과이며, 원본 워크북의 배포 상태인
 * {@link OriginalWorkbookStatus} 및 폐기된 {@link WorkbookStatus} 와는 별개다.
 */
public enum ChallengerWorkbookStatus {
    /**
     * 배포된 워크북 자체가 없음. 워크북 단건 조회에서는 나올 수 없고, 스터디원 목록처럼 미배포 인원이 행으로 포함되는 조회에서만 사용된다.
     */
    NOT_SUBMITTED,
    IN_PROGRESS,
    PASS,
    FAIL,
}
