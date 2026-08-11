package com.umc.product.inhouse.domain;

import java.time.LocalDate;

import com.umc.product.common.BaseEntity;
import com.umc.product.inhouse.domain.enums.UmcProductDepartmentRole;
import com.umc.product.inhouse.domain.enums.UmcProductPosition;
import com.umc.product.inhouse.domain.vo.UmcProductDatePeriod;
import com.umc.product.inhouse.exception.InhouseDomainException;
import com.umc.product.inhouse.exception.InhouseErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "umc_product_department_participant",
    indexes = {
        @Index(name = "ix_umc_product_department_participant_department", columnList = "department_id"),
        @Index(name = "ix_umc_product_department_participant_activity_period", columnList = "member_activity_period_id")
    }
)
public class UmcProductDepartmentParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private UmcProductDepartment department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_activity_period_id", nullable = false)
    private UmcProductMemberActivityPeriod memberActivityPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UmcProductDepartmentRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UmcProductPosition position;

    @Column(name = "responsibility_title", length = 200)
    private String responsibilityTitle;

    @Column(name = "responsibility_description", length = 1000)
    private String responsibilityDescription;

    @Embedded
    private UmcProductDatePeriod period;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductDepartmentParticipant(
        UmcProductDepartment department,
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductDatePeriod createdPeriod = UmcProductDatePeriod.of(startDate, endDate);
        validate(department, memberActivityPeriod, role, position, createdPeriod);
        this.department = department;
        this.memberActivityPeriod = memberActivityPeriod;
        this.role = role;
        this.position = position;
        this.responsibilityTitle = normalizeNullable(responsibilityTitle);
        this.responsibilityDescription = normalizeNullable(responsibilityDescription);
        this.period = createdPeriod;
    }

    public static UmcProductDepartmentParticipant create(
        UmcProductDepartment department,
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return UmcProductDepartmentParticipant.builder()
            .department(department)
            .memberActivityPeriod(memberActivityPeriod)
            .role(role)
            .position(position)
            .responsibilityTitle(responsibilityTitle)
            .responsibilityDescription(responsibilityDescription)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    public void update(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductMemberActivityPeriod nextActivityPeriod =
            memberActivityPeriod != null ? memberActivityPeriod : this.memberActivityPeriod;
        UmcProductDepartmentRole nextRole = role != null ? role : this.role;
        UmcProductPosition nextPosition = position != null ? position : this.position;
        LocalDate nextStartDate = startDate != null ? startDate : period.getStartDate();
        UmcProductDatePeriod nextPeriod = UmcProductDatePeriod.of(nextStartDate, endDate);
        validate(department, nextActivityPeriod, nextRole, nextPosition, nextPeriod);
        this.memberActivityPeriod = nextActivityPeriod;
        this.role = nextRole;
        this.position = nextPosition;
        this.responsibilityTitle = normalizeNullable(responsibilityTitle);
        this.responsibilityDescription = normalizeNullable(responsibilityDescription);
        this.period = nextPeriod;
    }

    public void updateResponsibility(String responsibilityTitle, String responsibilityDescription) {
        this.responsibilityTitle = normalizeNullable(responsibilityTitle);
        this.responsibilityDescription = normalizeNullable(responsibilityDescription);
    }

    public LocalDate getStartDate() {
        return period.getStartDate();
    }

    public LocalDate getEndDate() {
        return period.getEndDate();
    }

    public boolean isActiveOn(LocalDate date) {
        return period.isActiveOn(date);
    }

    private static void validate(
        UmcProductDepartment department,
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductDepartmentRole role,
        UmcProductPosition position,
        UmcProductDatePeriod period
    ) {
        if (department == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_DEPARTMENT_REQUIRED);
        }
        if (memberActivityPeriod == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        }
        if (role == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ROLE_REQUIRED);
        }
        if (position == null) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_POSITION_REQUIRED);
        }
        if (!memberActivityPeriod.contains(period.getStartDate(), period.getEndDate())
            || !department.contains(period.getStartDate(), period.getEndDate())) {
            throw new InhouseDomainException(InhouseErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
