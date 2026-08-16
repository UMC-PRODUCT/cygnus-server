package com.umc.product.curriculum.domain;

import java.time.Instant;
import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.curriculum.domain.enums.MissionType;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "mission_submission",
    uniqueConstraints = {
        @UniqueConstraint(
            // 원본 워크북의 미션 하나 당 제출물은 하나씩임
            // 챌린저 워크북은 인 당 하나만 배포되기 때문에 보장됨

            // 미션 하나에 제출 여러 개는 ... YANGNI ,,, 나중에 고민하는걸로
            name = "uk_mission_submission_original_wb_mission_challenger_wb_id",
            columnNames = {"original_workbook_mission_id", "challenger_workbook_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MissionSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_workbook_mission_id", nullable = false)
    private OriginalWorkbookMission originalWorkbookMission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenger_workbook_id", nullable = false)
    private ChallengerWorkbook challengerWorkbook;

    @Column(name = "submitted_mission_as_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private MissionType submittedAsType;

    // 고민: MissionType가 PLAIN 일 때, 즉 단순 제출일 떄를 위한 필드를 추가할 것인지
    // 아니면 content를 nullable로 둘 것인지?

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Builder(access = AccessLevel.PRIVATE)
    private MissionSubmission(
        OriginalWorkbookMission originalWorkbookMission,
        ChallengerWorkbook challengerWorkbook,
        MissionType submittedAsType,
        String content
    ) {
        this.originalWorkbookMission = originalWorkbookMission;
        this.challengerWorkbook = challengerWorkbook;
        this.submittedAsType = submittedAsType;
        this.content = content;
    }

    public static MissionSubmission create(
        OriginalWorkbookMission originalWorkbookMission,
        ChallengerWorkbook challengerWorkbook,
        String content
    ) {
        MissionType submittedAsType = originalWorkbookMission.getMissionType();
        validateContent(submittedAsType, content);

        return MissionSubmission.builder()
            .originalWorkbookMission(originalWorkbookMission)
            .challengerWorkbook(challengerWorkbook)
            .submittedAsType(submittedAsType)
            .content(content)
            .build();
    }

    public void edit(String content) {
        validateActive();
        validateContent(this.submittedAsType, content);
        this.content = content;
    }

    public void withdraw(Instant withdrawnAt) {
        validateActive();
        this.withdrawnAt = Objects.requireNonNull(withdrawnAt);
    }

    public boolean isSubmittedBy(Long memberId) {
        return Objects.equals(this.challengerWorkbook.getMemberId(), memberId);
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }

    private void validateActive() {
        if (isWithdrawn()) {
            throw new CurriculumDomainException(CurriculumErrorCode.MISSION_SUBMISSION_ALREADY_WITHDRAWN);
        }
    }

    private static void validateContent(MissionType submittedAsType, String content) {
        if (submittedAsType == MissionType.PLAIN) {
            return;
        }
        if (content == null || content.isBlank()) {
            throw new CurriculumDomainException(CurriculumErrorCode.SUBMISSION_REQUIRED);
        }
    }

}
