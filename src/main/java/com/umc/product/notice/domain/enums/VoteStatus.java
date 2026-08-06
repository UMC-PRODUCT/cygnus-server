package com.umc.product.notice.domain.enums;

/**
 * 투표의 공개 상태를 나타냅니다.
 * Form 도메인의 FormOpenStatus와는 독립적으로 관리됩니다.
 */
public enum VoteStatus {
    NOT_STARTED,  // 아직 시작하지 않음
    OPEN,         // 진행 중 (응답 가능)
    CLOSED        // 종료
}
