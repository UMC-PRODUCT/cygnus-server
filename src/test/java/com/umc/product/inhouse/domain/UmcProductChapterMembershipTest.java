package com.umc.product.inhouse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.umc.product.inhouse.domain.enums.UmcProductPosition;

class UmcProductChapterMembershipTest {

    @Test
    void 하는_일_수정은_책임_문자열만_정규화해_변경한다() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        UmcProductMemberActivityPeriod period = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create("홍길동", "길동", null, null, null),
            startDate,
            endDate
        );
        UmcProductChapter chapter = UmcProductChapter.create("SERVER", "Server", null, 1, true);
        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            period,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            "기존 책임",
            "기존 설명",
            startDate,
            endDate
        );

        membership.updateResponsibility("  API 개발  ", "   ");

        assertThat(membership.getResponsibilityTitle()).isEqualTo("API 개발");
        assertThat(membership.getResponsibilityDescription()).isNull();
        assertThat(membership.getMemberActivityPeriod()).isSameAs(period);
        assertThat(membership.getChapter()).isSameAs(chapter);
        assertThat(membership.getPosition()).isEqualTo(UmcProductPosition.SERVER_DEVELOPER);
        assertThat(membership.getStartDate()).isEqualTo(startDate);
        assertThat(membership.getEndDate()).isEqualTo(endDate);
    }
}
