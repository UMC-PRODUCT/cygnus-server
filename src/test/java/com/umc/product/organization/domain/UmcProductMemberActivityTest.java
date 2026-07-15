package com.umc.product.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.domain.enums.UmcProductLeadershipRole;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.exception.OrganizationErrorCode;

class UmcProductMemberActivityTest {

    private static final LocalDate JANUARY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DECEMBER_31 = LocalDate.of(2026, 12, 31);

    @Test
    void 멤버의_활동_기간과_그_안의_Chapter_소속을_생성한다() {
        UmcProductMember member = UmcProductMember.create(100L, " 소개 ", " image ");
        UmcProductMemberActivityPeriod activityPeriod =
            UmcProductMemberActivityPeriod.create(member, JANUARY_1, DECEMBER_31);
        UmcProductChapter chapter = chapter();

        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            activityPeriod,
            chapter,
            UmcProductPosition.SERVER_DEVELOPER,
            " API 설계 ",
            " 계약 관리 ",
            JANUARY_1,
            DECEMBER_31
        );

        assertThat(activityPeriod.getUmcProductMember()).isSameAs(member);
        assertThat(membership.getMemberActivityPeriod()).isSameAs(activityPeriod);
        assertThat(membership.getChapter()).isSameAs(chapter);
        assertThat(membership.getResponsibilityTitle()).isEqualTo("API 설계");
        assertThat(membership.isActiveOn(DECEMBER_31)).isTrue();
    }

    @Test
    void Chapter_소속_기간은_멤버_활동_기간_안에_있어야_한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();

        assertThatThrownBy(() -> UmcProductChapterMembership.create(
            activityPeriod,
            chapter(),
            UmcProductPosition.UNSPECIFIED,
            null,
            null,
            JANUARY_1.minusDays(1),
            DECEMBER_31
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    void Product_Leadership은_Chapter_소속과_독립적으로_생성한다() {
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create(100L, null, null),
            JANUARY_1,
            null
        );

        UmcProductLeadership leadership = UmcProductLeadership.create(
            activityPeriod,
            UmcProductLeadershipRole.UMC_PRODUCT_LEAD,
            JANUARY_1,
            null
        );

        assertThat(leadership.getMemberActivityPeriod()).isSameAs(activityPeriod);
        assertThat(leadership.getRole()).isEqualTo(UmcProductLeadershipRole.UMC_PRODUCT_LEAD);
        assertThat(leadership.getEndDate()).isNull();
    }

    @Test
    void Product_Leadership_기간도_멤버_활동_기간_안에_있어야_한다() {
        UmcProductMemberActivityPeriod activityPeriod = activityPeriod();

        assertThatThrownBy(() -> UmcProductLeadership.create(
            activityPeriod,
            UmcProductLeadershipRole.UMC_PRODUCT_VICE_LEAD,
            JANUARY_1,
            null
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    void 하위_활동을_수정하면_기간을_포함하는_다른_멤버_활동_기간으로_재연결할_수_있다() {
        UmcProductMember member = UmcProductMember.create(100L, null, null);
        UmcProductMemberActivityPeriod firstPeriod = UmcProductMemberActivityPeriod.create(
            member,
            JANUARY_1,
            LocalDate.of(2026, 6, 30)
        );
        UmcProductMemberActivityPeriod secondPeriod = UmcProductMemberActivityPeriod.create(
            member,
            LocalDate.of(2026, 7, 2),
            DECEMBER_31
        );
        UmcProductChapterMembership membership = UmcProductChapterMembership.create(
            firstPeriod,
            chapter(),
            UmcProductPosition.SERVER_DEVELOPER,
            null,
            null,
            JANUARY_1,
            LocalDate.of(2026, 6, 30)
        );

        membership.update(
            secondPeriod,
            null,
            null,
            null,
            null,
            LocalDate.of(2026, 7, 2),
            DECEMBER_31
        );

        assertThat(membership.getMemberActivityPeriod()).isSameAs(secondPeriod);
        assertThat(membership.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 2));
    }

    private UmcProductMemberActivityPeriod activityPeriod() {
        return UmcProductMemberActivityPeriod.create(
            UmcProductMember.create(100L, null, null),
            JANUARY_1,
            DECEMBER_31
        );
    }

    private UmcProductChapter chapter() {
        return UmcProductChapter.create("PLATFORM", "플랫폼", null, 1, true);
    }
}
