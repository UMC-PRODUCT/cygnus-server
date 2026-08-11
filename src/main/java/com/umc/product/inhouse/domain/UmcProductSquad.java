package com.umc.product.inhouse.domain;

import java.time.LocalDate;

import com.umc.product.common.BaseEntity;
import com.umc.product.inhouse.domain.vo.UmcProductDatePeriod;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "umc_product_squad")
public class UmcProductSquad extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Embedded
    private UmcProductDatePeriod period;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductSquad(
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean isActive
    ) {
        validate(code, name);
        this.code = normalizeRequired(code);
        this.name = normalizeRequired(name);
        this.description = normalizeNullable(description);
        this.period = UmcProductDatePeriod.of(startDate, endDate);
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public static UmcProductSquad create(
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        int sortOrder,
        boolean isActive
    ) {
        return UmcProductSquad.builder()
            .code(code)
            .name(name)
            .description(description)
            .startDate(startDate)
            .endDate(endDate)
            .sortOrder(sortOrder)
            .isActive(isActive)
            .build();
    }

    public void update(
        String code,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        Integer sortOrder,
        Boolean isActive
    ) {
        String nextCode = code != null ? code : this.code;
        String nextName = name != null ? name : this.name;
        LocalDate nextStartDate = startDate != null ? startDate : period.getStartDate();
        validate(nextCode, nextName);
        this.code = normalizeRequired(nextCode);
        this.name = normalizeRequired(nextName);
        if (description != null) {
            this.description = normalizeNullable(description);
        }
        this.period = UmcProductDatePeriod.of(nextStartDate, endDate);
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
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

    public boolean isActiveOn(LocalDate date) {
        return isActive && period.isActiveOn(date);
    }

    private static void validate(String code, String name) {
        if (code == null || code.isBlank()) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_CODE_REQUIRED);
        }
        if (name == null || name.isBlank()) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_SQUAD_NAME_REQUIRED);
        }
    }

    private static String normalizeRequired(String value) {
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
