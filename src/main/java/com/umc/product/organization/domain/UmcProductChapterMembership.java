package com.umc.product.organization.domain;

import java.time.LocalDate;

import com.umc.product.common.BaseEntity;
import com.umc.product.organization.domain.enums.UmcProductPosition;
import com.umc.product.organization.domain.vo.UmcProductDatePeriod;
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
    name = "umc_product_chapter_membership",
    indexes = {
        @Index(name = "ix_umc_product_chapter_membership_activity_period", columnList = "member_activity_period_id"),
        @Index(name = "ix_umc_product_chapter_membership_chapter", columnList = "chapter_id")
    }
)
public class UmcProductChapterMembership extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_activity_period_id", nullable = false)
    private UmcProductMemberActivityPeriod memberActivityPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private UmcProductChapter chapter;

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
    private UmcProductChapterMembership(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductChapter chapter,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductDatePeriod createdPeriod = UmcProductDatePeriod.of(startDate, endDate);
        validate(memberActivityPeriod, chapter, position, createdPeriod);
        this.memberActivityPeriod = memberActivityPeriod;
        this.chapter = chapter;
        this.position = position;
        this.responsibilityTitle = normalizeNullable(responsibilityTitle);
        this.responsibilityDescription = normalizeNullable(responsibilityDescription);
        this.period = createdPeriod;
    }

    public static UmcProductChapterMembership create(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductChapter chapter,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        return UmcProductChapterMembership.builder()
            .memberActivityPeriod(memberActivityPeriod)
            .chapter(chapter)
            .position(position)
            .responsibilityTitle(responsibilityTitle)
            .responsibilityDescription(responsibilityDescription)
            .startDate(startDate)
            .endDate(endDate)
            .build();
    }

    public void update(
        UmcProductMemberActivityPeriod memberActivityPeriod,
        UmcProductChapter chapter,
        UmcProductPosition position,
        String responsibilityTitle,
        String responsibilityDescription,
        LocalDate startDate,
        LocalDate endDate
    ) {
        UmcProductMemberActivityPeriod nextActivityPeriod =
            memberActivityPeriod != null ? memberActivityPeriod : this.memberActivityPeriod;
        UmcProductChapter nextChapter = chapter != null ? chapter : this.chapter;
        UmcProductPosition nextPosition = position != null ? position : this.position;
        LocalDate nextStartDate = startDate != null ? startDate : period.getStartDate();
        UmcProductDatePeriod nextPeriod = UmcProductDatePeriod.of(nextStartDate, endDate);
        validate(nextActivityPeriod, nextChapter, nextPosition, nextPeriod);
        this.memberActivityPeriod = nextActivityPeriod;
        this.chapter = nextChapter;
        this.position = nextPosition;
        this.responsibilityTitle = normalizeNullable(responsibilityTitle);
        this.responsibilityDescription = normalizeNullable(responsibilityDescription);
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
        UmcProductChapter chapter,
        UmcProductPosition position,
        UmcProductDatePeriod period
    ) {
        if (memberActivityPeriod == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_REQUIRED);
        }
        if (chapter == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_CHAPTER_REQUIRED);
        }
        if (position == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_POSITION_REQUIRED);
        }
        if (!memberActivityPeriod.contains(period.getStartDate(), period.getEndDate())) {
            throw new OrganizationDomainException(OrganizationErrorCode.UMC_PRODUCT_ACTIVITY_PERIOD_OUT_OF_RANGE);
        }
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
