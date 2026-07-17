package com.umc.product.organization.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.springframework.util.StringUtils;

import com.umc.product.common.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "school")
public class School extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "school", orphanRemoval = true, cascade = CascadeType.ALL)
    List<ChapterSchool> chapterSchools;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String logoImageId;

    @Column(nullable = true)
    private String remark;

    @BatchSize(size = 10)
    @OneToMany(mappedBy = "school", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<SchoolLink> schoolLinks = new ArrayList<>();

    @Builder(access = AccessLevel.PRIVATE)
    private School(String name, String remark, ArrayList<ChapterSchool> chapterSchools) {
        this.name = name;
        this.remark = remark;
        this.chapterSchools = chapterSchools;
    }

    public static School create(String name, String remark) {
        return School.builder()
            .name(name)
            .remark(remark)
            .chapterSchools(new ArrayList<>())
            .build();
    }

    public void updateLogoImageId(String logoImageId) {
        if (logoImageId != null) {
            this.logoImageId = StringUtils.hasText(logoImageId) ? logoImageId : null;
        }
    }

    public void updateName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name;
        }
    }

    public void updateRemark(String remark) {
        if (StringUtils.hasText(remark)) {
            this.remark = remark;
        }
    }

    public void updateChapterSchool(Chapter newChapter) {
        this.chapterSchools.removeIf(cs -> cs.getChapter().getGisu().isActive());
        ChapterSchool chapterSchool = ChapterSchool.create(newChapter, this);
        this.chapterSchools.add(chapterSchool);
    }

    public void assignToChapter(Chapter chapter) {
        Long gisuId = chapter.getGisu().getId();
        this.chapterSchools.removeIf(cs -> cs.getChapter().getGisu().getId().equals(gisuId));
        ChapterSchool chapterSchool = ChapterSchool.create(chapter, this);
        this.chapterSchools.add(chapterSchool);
    }

    public void unassignFromGisu(Long gisuId) {
        this.chapterSchools.removeIf(cs -> cs.getChapter().getGisu().getId().equals(gisuId));
    }

    public void updateLinks(List<SchoolLink> newLinks) {
        this.schoolLinks.clear();
        this.schoolLinks.addAll(newLinks);
    }
}
