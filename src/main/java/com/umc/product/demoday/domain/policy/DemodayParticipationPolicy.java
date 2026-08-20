package com.umc.product.demoday.domain.policy;

/**
 * 데모데이 참여자가 스탬프 수집 단계를 완료하고 투표 권한을 요청할 수 있는지를 판단하는 정책이다.
 *
 * <p>현재 정책의 기준값은 유효 스탬프 개수지만 이 클래스의 책임은 스탬프의 저장·취소·적립 간격이 아니라
 * 스탬프 수집 결과와 투표 슬롯 사용 여부를 함께 해석해 참여 흐름의 다음 단계로 이동할 수 있는지를 결정하는 것이다.
 * 따라서 스탬프 정책이 아닌 참여 정책으로 명명한다.
 */
public final class DemodayParticipationPolicy {

    private static final int REQUIRED_STAMP_COUNT = 6;

    private DemodayParticipationPolicy() {
    }

    public static int requiredStampCount() {
        return REQUIRED_STAMP_COUNT;
    }

    public static boolean hasRequiredStamps(long activeStampCount) {
        return activeStampCount >= REQUIRED_STAMP_COUNT;
    }

    public static boolean canRequestVoteAuthorization(long activeStampCount, boolean hasUsedVoteSlot) {
        return hasRequiredStamps(activeStampCount) && !hasUsedVoteSlot;
    }
}
