package com.umc.product.organization.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.organization.exception.OrganizationDomainException;
import com.umc.product.organization.exception.OrganizationErrorCode;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "chapter_school")
public class ChapterSchool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Builder(access = AccessLevel.PRIVATE)
    private ChapterSchool(Chapter chapter, School school) {
        validate(chapter, school);
        this.chapter = chapter;
        this.school = school;
    }

    public static ChapterSchool create(Chapter chapter, School school) {
        return ChapterSchool.builder()
            .chapter(chapter)
            .school(school)
            .build();
    }

    private static void validate(Chapter chapter, School school) {
        if (chapter == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.CHAPTER_REQUIRED);
        }
        if (school == null) {
            throw new OrganizationDomainException(OrganizationErrorCode.SCHOOL_REQUIRED);
        }
    }
}
