package com.umc.product.recruiting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.umc.product.recruiting.domain.exception.RecruitingDomainException;
import com.umc.product.recruiting.domain.exception.RecruitingErrorCode;

class RecruitingChapterQuotaTest {

    @Test
    @DisplayName("지부 전체 TO는 0명으로 생성할 수 있다")
    void createAllowsZeroTargetCount() {
        RecruitingChapterQuota quota = RecruitingChapterQuota.create(1L, 2L, 0);

        assertThat(quota.getTotalTargetCount()).isZero();
    }

    @Test
    @DisplayName("지부 전체 TO는 음수로 변경할 수 없다")
    void updateRejectsNegativeTargetCount() {
        RecruitingChapterQuota quota = RecruitingChapterQuota.create(1L, 2L, 3);

        assertThatThrownBy(() -> quota.updateTotalTargetCount(-1))
            .isInstanceOf(RecruitingDomainException.class)
            .extracting("baseCode")
            .isEqualTo(RecruitingErrorCode.RECRUITING_CHAPTER_QUOTA_INVALID_TARGET_COUNT);
    }
}
