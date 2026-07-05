package com.umc.product.curriculum.domain;

import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.exception.CurriculumDomainException;
import com.umc.product.curriculum.domain.exception.CurriculumErrorCode;

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
@Table(
    name = "challenger_workbook",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_challenger_workbook_member_id_original_workbook_id",
        columnNames = {"member_id", "original_workbook_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengerWorkbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "study_group_id")
    private Long studyGroupId; // 워크북 강제 배포등을 고려하여 nullable로 함.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_workbook_id", nullable = false)
    private OriginalWorkbook originalWorkbook;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isExcused; // true일시 과제 제출 여부와 무관하게 PASS처리

    private String excusedReason;
    private Long excuseApprovedMemberId;

    @Builder(access = AccessLevel.PRIVATE)
    private ChallengerWorkbook(
        OriginalWorkbook originalWorkbook,
        Long memberId,
        Long studyGroupId
    ) {
        if (originalWorkbook == null) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.WORKBOOK_NOT_FOUND,
                "배포할 원본 워크북을 찾을 수 없어요. 원본 워크북을 확인해주세요."
            );
        }
        if (memberId == null) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }
        this.originalWorkbook = originalWorkbook;
        this.memberId = memberId;
        this.studyGroupId = studyGroupId;
        this.isExcused = false;
    }

    public static ChallengerWorkbook create(
        OriginalWorkbook originalWorkbook,
        Long memberId,
        Long studyGroupId
    ) {
        return ChallengerWorkbook.builder()
            .originalWorkbook(originalWorkbook)
            .memberId(memberId)
            .studyGroupId(studyGroupId)
            .build();
    }

    public void edit(String content) {
        if (!hasText(content)) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.SUBMISSION_REQUIRED,
                "워크북 내용을 입력해주세요."
            );
        }
        this.content = content;
    }

    public void excuse(String reason, Long excuseApprovedMemberId) {
        if (!hasText(reason)) {
            throw new CurriculumDomainException(
                CurriculumErrorCode.SUBMISSION_REQUIRED,
                "인정 처리 사유를 입력해주세요."
            );
        }
        if (excuseApprovedMemberId == null) {
            throw new CurriculumDomainException(CurriculumErrorCode.WORKBOOK_ACCESS_DENIED);
        }

        this.isExcused = true;
        this.excusedReason = reason;
        this.excuseApprovedMemberId = excuseApprovedMemberId;
    }

    public boolean isOwnedBy(Long memberId) {
        return Objects.equals(this.memberId, memberId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
