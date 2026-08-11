package com.umc.product.inhouse.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.umc.product.global.exception.BusinessException;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.exception.InhouseErrorCode;

class UmcProductDepartmentTest {

    private static final LocalDate JANUARY_1 = LocalDate.of(2026, 1, 1);
    private static final LocalDate DECEMBER_31 = LocalDate.of(2026, 12, 31);

    @Test
    void Department는_필수_시작일과_nullable_종료일을_가진다() {
        UmcProductDepartment department = UmcProductDepartment.create(
            "RECRUIT",
            "모집 Department",
            null,
            null,
            JANUARY_1,
            null,
            1,
            true
        );

        assertThat(department.getStartDate()).isEqualTo(JANUARY_1);
        assertThat(department.getEndDate()).isNull();
        assertThat(department.isActiveOn(LocalDate.of(2099, 12, 31))).isTrue();
    }

    @Test
    void Department의_종료일이_시작일보다_빠르면_생성할_수_없다() {
        assertThatThrownBy(() -> UmcProductDepartment.create(
            "INVALID",
            "잘못된 Department",
            null,
            null,
            DECEMBER_31,
            JANUARY_1,
            1,
            true
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_PERIOD_INVALID);
    }

    @Test
    void Department_참여_기간은_멤버와_Department_기간_모두에_포함되어야_한다() {
        UmcProductDepartment department = department();
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create("테스트", "테스터", null, null, null),
            JANUARY_1,
            DECEMBER_31
        );

        assertThatThrownBy(() -> UmcProductDepartmentParticipant.create(
            department,
            activityPeriod,
            UmcProductDepartmentRole.MEMBER,
            UmcProductPosition.PRODUCT_DESIGNER,
            null,
            null,
            JANUARY_1.minusDays(1),
            JANUARY_1
        ))
            .isInstanceOf(BusinessException.class)
            .extracting("baseCode")
            .isEqualTo(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
    }

    @Test
    void Department_참여자는_역할과_책임_그리고_자체_기간을_가진다() {
        UmcProductDepartment department = department();
        UmcProductMemberActivityPeriod activityPeriod = UmcProductMemberActivityPeriod.create(
            UmcProductMember.create("테스트", "테스터", null, null, null),
            JANUARY_1,
            DECEMBER_31
        );

        UmcProductDepartmentParticipant participant = UmcProductDepartmentParticipant.create(
            department,
            activityPeriod,
            UmcProductDepartmentRole.DEPARTMENT_LEAD,
            UmcProductPosition.PRODUCT_OWNER,
            "정책 정리",
            "요구사항 관리",
            JANUARY_1,
            DECEMBER_31
        );

        assertThat(participant.getDepartment()).isSameAs(department);
        assertThat(participant.getMemberActivityPeriod()).isSameAs(activityPeriod);
        assertThat(participant.getRole()).isEqualTo(UmcProductDepartmentRole.DEPARTMENT_LEAD);
        assertThat(participant.getStartDate()).isEqualTo(JANUARY_1);
        assertThat(participant.getEndDate()).isEqualTo(DECEMBER_31);
    }

    private UmcProductDepartment department() {
        return UmcProductDepartment.create(
            "RECRUIT",
            "모집 Department",
            null,
            null,
            JANUARY_1,
            DECEMBER_31,
            1,
            true
        );
    }
}
