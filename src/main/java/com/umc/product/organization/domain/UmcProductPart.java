package com.umc.product.organization.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "umc_product_part",
    uniqueConstraints = @UniqueConstraint(name = "uk_umc_product_part_chapter_code", columnNames = {"chapter_id", "code"})
)
public class UmcProductPart extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false, updatable = false)
    private UmcProductChapter chapter;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Builder(access = AccessLevel.PRIVATE)
    private UmcProductPart(
        UmcProductChapter chapter,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean isActive
    ) {
        validate(chapter, code, name);
        this.chapter = chapter;
        this.code = normalizeRequired(code);
        this.name = normalizeRequired(name);
        this.description = normalizeNullable(description);
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public static UmcProductPart create(
        UmcProductChapter chapter,
        String code,
        String name,
        String description,
        int sortOrder,
        boolean isActive
    ) {
        return UmcProductPart.builder()
            .chapter(chapter)
            .code(code)
            .name(name)
            .description(description)
            .sortOrder(sortOrder)
            .isActive(isActive)
            .build();
    }

    public void update(
        String code,
        String name,
        String description,
        Integer sortOrder,
        Boolean isActive
    ) {
        String nextCode = code != null ? code : this.code;
        String nextName = name != null ? name : this.name;
        validate(this.chapter, nextCode, nextName);
        this.code = normalizeRequired(nextCode);
        this.name = normalizeRequired(nextName);
        if (description != null) {
            this.description = normalizeNullable(description);
        }
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
        if (isActive != null) {
            this.isActive = isActive;
        }
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    private static void validate(UmcProductChapter chapter, String code, String name) {
        if (chapter == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_REQUIRED);
        }
        if (code == null || code.isBlank()) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_CODE_REQUIRED);
        }
        if (name == null || name.isBlank()) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_PART_NAME_REQUIRED);
        }
    }

    private static String normalizeRequired(String value) {
        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
