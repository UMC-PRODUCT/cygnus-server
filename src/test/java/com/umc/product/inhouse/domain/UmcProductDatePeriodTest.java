package com.umc.product.inhouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.domain.vo.UmcProductDatePeriod;
import com.umc.product.organization.exception.OrganizationErrorCode;

class UmcProductDatePeriodTest {

    private static final LocalDate JANUARY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate JANUARY_31 = LocalDate.of(2026, 1, 31);

    @Test
    void 같은_날_시작하고_종료하는_기간을_생성할_수_있다() {
        UmcProductDatePeriod period = UmcProductDatePeriod.of(JANUARY_1, JANUARY_1);

        assertThat(period.getStartDate()).isEqualTo(JANUARY_1);
        assertThat(period.getEndDate()).isEqualTo(JANUARY_1);
        assertThat(period.isActiveOn(JANUARY_1)).isTrue();
    }

    @Test
    void 종료일이_없는_진행_중_기간을_생성할_수_있다() {
        UmcProductDatePeriod period = UmcProductDatePeriod.of(JANUARY_1, null);

        assertThat(period.getEndDate()).isNull();
        assertThat(period.isActiveOn(LocalDate.of(2099, 12, 31))).isTrue();
    }

    @Test
    void 시작일이_없으면_기간을_생성할_수_없다() {
        assertThatThrownBy(() -> UmcProductDatePeriod.of(null, JANUARY_31))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
    }

    @Test
    void 종료일이_시작일보다_빠르면_기간을_생성할_수_없다() {
        assertThatThrownBy(() -> UmcProductDatePeriod.of(JANUARY_31, JANUARY_1))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    void 기간_포함과_겹침은_종료일을_포함해서_판단한다() {
        UmcProductDatePeriod period = UmcProductDatePeriod.of(JANUARY_1, JANUARY_31);
        UmcProductDatePeriod sameEndDate = UmcProductDatePeriod.of(JANUARY_31, LocalDate.of(2026, 2, 1));

        assertThat(period.contains(JANUARY_1, JANUARY_31)).isTrue();
        assertThat(period.overlaps(sameEndDate)).isTrue();
    }

    @Test
    void 빈_날짜_없이_바로_이어진_기간을_인접한_기간으로_판단한다() {
        UmcProductDatePeriod period = UmcProductDatePeriod.of(JANUARY_1, JANUARY_31);
        UmcProductDatePeriod adjacent = UmcProductDatePeriod.of(LocalDate.of(2026, 2, 1), null);

        assertThat(period.overlaps(adjacent)).isFalse();
        assertThat(period.isAdjacentTo(adjacent)).isTrue();
    }
}
