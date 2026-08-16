package com.umc.product.curriculum.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.enums.MissionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "original_workbook_mission")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class OriginalWorkbookMission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_workbook_id", nullable = false)
    private OriginalWorkbook originalWorkbook;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionType missionType;

    @Column(nullable = false)
    private boolean isNecessary;

    @Builder(access = AccessLevel.PRIVATE)
    private OriginalWorkbookMission(
        OriginalWorkbook originalWorkbook,
        String title,
        String description,
        MissionType missionType,
        boolean isNecessary
    ) {
        this.originalWorkbook = originalWorkbook;
        this.title = title;
        this.description = description;
        this.missionType = missionType;
        this.isNecessary = isNecessary;
    }

    public static OriginalWorkbookMission create(
        OriginalWorkbook originalWorkbook,
        String title,
        String description,
        MissionType missionType,
        boolean isNecessary
    ) {
        return OriginalWorkbookMission.builder()
            .originalWorkbook(originalWorkbook)
            .title(title)
            .description(description)
            .missionType(missionType)
            .isNecessary(isNecessary)
            .build();
    }

    public void edit(String title, String description, MissionType missionType, Boolean isNecessary) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (missionType != null) {
            this.missionType = missionType;
        }
        if (isNecessary != null) {
            this.isNecessary = isNecessary;
        }
    }
}
