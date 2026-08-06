package com.umc.product.organization.domain;

import java.time.LocalDate;

import com.umc.product.common.BaseEntity;
import com.umc.product.organization.domain.vo.UmcProductDatePeriod;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
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
    name = "umc_product_member_activity_period",
    indexes = @Index(name = "ix_umc_product_member_activity_period_member", columnList = "umc_product_member_id")
)
public class UmcProductMemberActivityPeriod extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "umc_product_member_id", nullable = false, updatable = false)
    private UmcProductMember umcProductMember;

    @Embedded
    private UmcProductDatePeriod period;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductMemberActivityPeriod(
        UmcProductMember umcProductMember,
        LocalDate startDate,
        LocalDate endDate
    ) {
        validateMember(umcProductMember);
        this.umcProductMember = umcProductMember;
        this.period = UmcProductDatePeriod.of(startDate, endDate);
    }

    public static UmcProductMemberActivityPeriod create(
        UmcProductMember umcProductMember,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return UmcProductMemberActivityPeriod.builder()
            .umcProductMember(umcProductMember)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    public void updatePeriod(LocalDate startDate, LocalDate endDate) {
        LocalDate nextStartDate = startDate != null ? startDate : period.getStartDate();
        this.period = UmcProductDatePeriod.of(nextStartDate, endDate);
    }

    public LocalDate getStartDate() {
        return period.getStartDate();
    }

    public LocalDate getEndDate() {
        return period.getEndDate();
    }

    public boolean contains(LocalDate startDate, LocalDate endDate) {
        return period.contains(startDate, endDate);
    }

    public boolean overlaps(UmcProductMemberActivityPeriod other) {
        return other != null && period.overlaps(other.period);
    }

    public boolean isAdjacentTo(UmcProductMemberActivityPeriod other) {
        return other != null && period.isAdjacentTo(other.period);
    }

    public boolean isActiveOn(LocalDate date) {
        return period.isActiveOn(date);
    }

    private static void validateMember(UmcProductMember umcProductMember) {
        if (umcProductMember == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_MEMBER_REQUIRED);
        }
    }
}
