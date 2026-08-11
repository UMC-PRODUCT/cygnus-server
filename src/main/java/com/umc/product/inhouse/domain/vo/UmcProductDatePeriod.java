package com.umc.product.inhouse.domain.vo;

import java.time.LocalDate;

import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UmcProductDatePeriod {

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private UmcProductDatePeriod(LocalDate startDate, LocalDate endDate) {
        validate(startDate, endDate);
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public static UmcProductDatePeriod of(LocalDate startDate, LocalDate endDate) {
        return new UmcProductDatePeriod(startDate, endDate);
    }

    public boolean contains(UmcProductDatePeriod other) {
        if (other == null || other.startDate.isBefore(startDate)) {
            return false;
        }
        if (endDate == null) {
            return true;
        }
        return other.endDate != null && !other.endDate.isAfter(endDate);
    }

    public boolean contains(LocalDate otherStartDate, LocalDate otherEndDate) {
        return contains(of(otherStartDate, otherEndDate));
    }

    public boolean overlaps(UmcProductDatePeriod other) {
        if (other == null) {
            return false;
        }
        return !endsBefore(other) && !other.endsBefore(this);
    }

    public boolean isAdjacentTo(UmcProductDatePeriod other) {
        if (other == null) {
            return false;
        }
        return isImmediatelyBefore(this, other) || isImmediatelyBefore(other, this);
    }

    public boolean isActiveOn(LocalDate date) {
        if (date == null || date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private boolean endsBefore(UmcProductDatePeriod other) {
        return endDate != null && endDate.isBefore(other.startDate);
    }

    private static boolean isImmediatelyBefore(UmcProductDatePeriod first, UmcProductDatePeriod second) {
        return first.endDate != null
            && !first.endDate.equals(LocalDate.MAX)
            && first.endDate.plusDays(1).equals(second.startDate);
    }

    private static void validate(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_START_DATE_REQUIRED);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PERIOD_INVALID);
        }
    }
}
