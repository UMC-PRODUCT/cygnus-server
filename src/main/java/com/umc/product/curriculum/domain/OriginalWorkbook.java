package com.umc.product.curriculum.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookStatus;
import com.umc.product.curriculum.domain.enums.OriginalWorkbookType;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

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
@Table(name = "original_workbook")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginalWorkbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weekly_curriculum_id", nullable = false)
    private WeeklyCurriculum weeklyCurriculum;

    @Column(name = "title", nullable = false)
    private String title;

    // 워크북에 대한 간단한 설명
    @Column(name = "description")
    private String description;

    @Column(name = "url")
    private String url;

    //    OriginalWorkbookStatus이 DRAFT, FINAL이 있다면 FINAL만 release할 수 있도록 할 지?
    //    주차를 한번에 release하게할 까요 originalWorkbook을 하나씩 release하게 할까 고민
    @Enumerated(EnumType.STRING)
    @Column(name = "original_workbook_status", nullable = false)
    private OriginalWorkbookStatus originalWorkbookStatus;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    private Instant releasedAt;
    private Long releasedMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OriginalWorkbookType type;

    @Builder(access = AccessLevel.PRIVATE)
    private OriginalWorkbook(
        WeeklyCurriculum weeklyCurriculum,
        String title, String description,
        String url, String content,
        OriginalWorkbookType type,
        OriginalWorkbookStatus originalWorkbookStatus
    ) {
        this.weeklyCurriculum = weeklyCurriculum;
        this.title = title;
        this.description = description;
        this.url = url;
        this.content = content;
        this.type = type;
        this.originalWorkbookStatus = originalWorkbookStatus;
    }

    /**
     * 원본 워크북 생성 (임시저장: DRAFT 상태)
     */
    public static OriginalWorkbook createAsDraft(
        WeeklyCurriculum weeklyCurriculum, String title, String description,
        String url, String content, OriginalWorkbookType type
    ) {
        return OriginalWorkbook.builder()
            .weeklyCurriculum(weeklyCurriculum)
            .title(title)
            .description(description)
            .url(url)
            .content(content)
            .type(type)
            .originalWorkbookStatus(OriginalWorkbookStatus.DRAFT)
            .build();
    }

    /**
     * 원본 워크북 생성 (배포 준비: READY 상태)
     * <p>
     * READY 상태로 생성된 워크북은 스케줄러에 의해 배포 시점에 자동 배포될 수 있습니다.
     */
    public static OriginalWorkbook createAsReady(
        WeeklyCurriculum weeklyCurriculum, String title, String description,
        String url, String content, OriginalWorkbookType type
    ) {
        return OriginalWorkbook.builder()
            .weeklyCurriculum(weeklyCurriculum)
            .title(title)
            .description(description)
            .url(url)
            .content(content)
            .type(type)
            .originalWorkbookStatus(OriginalWorkbookStatus.READY)
            .build();
    }

    public void edit(
        String title,
        String description,
        String url,
        String content
    ) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (url != null) {
            this.url = url;
        }
        if (content != null) {
            this.content = content;
        }
    }

    /**
     * 원본 워크북 상태 전환
     * <p>
     * 허용된 전환만 가능합니다:
     * <ul>
     *   <li>DRAFT → READY (배포 준비)</li>
     *   <li>READY → RELEASED (배포 완료, releasedAt/releasedMemberId 기록)</li>
     *   <li>READY → DRAFT (임시저장으로 롤백)</li>
     *   <li>RELEASED → any: 불가 (배포 완료 후 되돌리기 불가)</li>
     * </ul>
     *
     * @param newStatus         전환할 목표 상태
     * @param requestedMemberId 요청 운영진의 멤버 ID (RELEASED 전환 시 releasedMemberId로 기록)
     * @throws CurriculumDomainException 허용되지 않는 전환인 경우
     */
    public void changeStatus(OriginalWorkbookStatus newStatus, Long requestedMemberId) {
        if (!this.originalWorkbookStatus.canTransitionTo(newStatus)) {
            throw new CurriculumDomainException(CurriculumErrorCode.INVALID_WORKBOOK_STATUS_TRANSITION);
        }
        this.originalWorkbookStatus = newStatus;
        if (newStatus == OriginalWorkbookStatus.RELEASED) {
            this.releasedAt = Instant.now();
            this.releasedMemberId = requestedMemberId;
        }
    }
}
