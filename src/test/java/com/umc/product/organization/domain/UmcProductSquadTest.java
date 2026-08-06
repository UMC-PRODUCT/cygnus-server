package com.umc.product.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.enums.UmcProductSquadRole;
import com.umc.product.organization.exception.OrganizationErrorCode;

class UmcProductSquadTest {

    private static final LocalDate JANUARY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DECEMBER_31 = LocalDate.of(2026, 12, 31);

    @Test
    void Squad는_필수_시작일과_nullable_종료일을_가진다() {
        UmcProductSquad squad = UmcProductSquad.create(
            "RECRUIT",
            "모집 Squad",
            null,
            JANUARY_1,
            null,
            1,
            true
        );

        assertThat(squad.getStartDate()).isEqualTo(JANUARY_1);
        assertThat(squad.getEndDate()).isNull();
        assertThat(squad.isActiveOn(LocalDate.of(2099, 12, 31))).isTrue();
    }

    @Test
    void Squad의_종료일이_시작일보다_빠르면_생성할_수_없다() {
        assertThatThrownBy(() -> UmcProductSquad.create(
            "INVALID",
            "잘못된 Squad",
            null,
            DECEMBER_31,
            JANUARY_1,
            1,
            true
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    void Squad_참여_기간은_멤버와_Squad_기간_모두에_포함되어야_한다() {
        UmcProductSquad squad = squad();
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create(100L, null, null),
            JANUARY_1,
            DECEMBER_31
        );

        assertThatThrownBy(() -> UmcProductSquadParticipant.create(
            squad,
            activityPeriod,
            UmcProductSquadRole.MEMBER,
            UmcProductPosition.PRODUCT_DESIGNER,
            null,
            null,
            JANUARY_1.minusDays(1),
            JANUARY_1
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    void Squad_참여자는_역할과_책임_그리고_자체_기간을_가진다() {
        UmcProductSquad squad = squad();
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create(100L, null, null),
            JANUARY_1,
            DECEMBER_31
        );

        UmcProductSquadParticipant participant = UmcProductSquadParticipant.create(
            squad,
            activityPeriod,
            UmcProductSquadRole.SQUAD_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "정책 정리",
            "요구사항 관리",
            JANUARY_1,
            DECEMBER_31
        );

        assertThat(participant.getSquad()).isSameAs(squad);
        assertThat(participant.getMemberActivityPeriod()).isSameAs(activityPeriod);
        assertThat(participant.getRole()).isEqualTo(UmcProductSquadRole.SQUAD_LEAD);
        assertThat(participant.getStartDate()).isEqualTo(JANUARY_1);
        assertThat(participant.getEndDate()).isEqualTo(DECEMBER_31);
    }

    private UmcProductSquad squad() {
        return UmcProductSquad.create(
            "RECRUIT",
            "모집 Squad",
            null,
            JANUARY_1,
            DECEMBER_31,
            1,
            true
        );
    }
}
