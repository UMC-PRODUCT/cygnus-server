package com.umc.product.inhouse.domain;

import java.time.LocalDate;

import com.umc.product.common.BaseEntity;
import com.umc.product.inhouse.domain.enums.UmcProductLeadershipRole;
import com.umc.product.inhouse.domain.vo.UmcProductDatePeriod;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

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
    name = "umc_product_leadership",
    indexes = {
        @Index(name = "ix_umc_product_leadership_activity_period", columnList = "member_activity_period_id"),
        @Index(name = "ix_umc_product_leadership_role", columnList = "role")
    }
)
public class UmcProductLeadership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_activity_period_id", nullable = false)
    private UmcProductMemberActivityPeriod memberActivityPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UmcProductLeadershipRole role;

    @Embedded
    private UmcProductDatePeriod period;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductLeadership(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductDatePeriod createdPeriod = UmcProductDatePeriod.of(startDate, endDate);
        validate(memberActivityPeriod, role, createdPeriod);
        this.memberActivityPeriod = memberActivityPeriod;
        this.role = role;
        this.period = createdPeriod;
    }

    public static UmcProductLeadership create(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return UmcProductLeadership.builder()
            .memberActivityPeriod(memberActivityPeriod)
            .role(role)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    public void update(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductLeadershipRole role,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductMemberActivityPeriod nextActivityPeriod =
            memberActivityPeriod != null ? memberActivityPeriod : this.memberActivityPeriod;
        UmcProductLeadershipRole nextRole = role != null ? role : this.role;
        LocalDate nextStartDate = startDate != null ? startDate : period.getStartDate();
        UmcProductDatePeriod nextPeriod = UmcProductDatePeriod.of(nextStartDate, endDate);
        validate(nextActivityPeriod, nextRole, nextPeriod);
        this.memberActivityPeriod = nextActivityPeriod;
        this.role = nextRole;
        this.period = nextPeriod;
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
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductLeadershipRole role,
        UmcProductDatePeriod period
    ) {
        if (memberActivityPeriod == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        }
        if (role == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_LEADERSHIP_ROLE_REQUIRED);
        }
        if (!memberActivityPeriod.contains(period.getStartDate(), period.getEndDate())) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }
}
