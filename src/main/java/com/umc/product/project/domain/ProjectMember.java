package com.umc.product.project.domain;

import java.time.Instant;

import com.umc.product.common.BaseEntity;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.project.domain.enums.ProjectMemberStatus;

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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "project_member",
    // 프로젝트 내에 멤버는 고유합니다.
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_project_member_project_member",
            columnNames = {"project_id", "member_id"}
        )
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 기존 고려사항.
    // 정상적으로 지원해서 들어온 경우, 제출한 지원서의 ID값(FormResponseId)을 가지고 있어야 합니다.
    // 랜덤 매칭이나, 운영진에 의해서 강제 배정된 경우에는 지원서 ID 값이 null이 될 수 있습니다.
    // 운영진에 의한 경우는, 누가 어떤 작업을 수행했는지에 대한 로그를 남겨야 합니다.

    // 변경된 고려사항: formResponseId를 직접적으로 가지고 있는 순간,
    // "ProjectMember"라는 프로젝트에 어떤 인원이 있는지를 나타내는 엔티티가 지원서라는 외부의 개념에 강하게 결합됩니다.
    // 특정 사용자가 어떤 지원서를 작성했는지는 Service 단에서 해결해주는 것으로 하고, 엔티티 단에서는 결합을 끊어놓는 방향이 긍정적으로 판단됩니다.

    // 변경된 고려사항 2: 그래도 직접적으로 연계는 해놓는 것이 추후 정합성을 검증하는데 도움이 될 것으로 판단됨.
    // 어떤 지원서로 붙은건지도 확인하면 재밌을 것 같음.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_application_id")
    private ProjectApplication application; // 어떤 지원서로 들어왔는지, nullable true!


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private Long memberId;

    // 보조 PM은 PLAN 파트로 저장됩니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengerPart part;

    // 리더 여부를 나타냅니다. 추후 리더에 대해서 특정 권한을 부여하거나, 뱃지 등을 필요로 할 때 활용될 수 있습니다.
    @Column(nullable = false)
    private boolean isLeader = false; // 사용하지 않을 수도 있는 기능이라 defaults to false.

    private String description; // 수행한 역할 등의 개인별 설명을 추가할 수 있도록 하는 필드입니다.

    // Plan 챌린저가 아닌 운영진이 직접 추가한 경우를 고려합니다.
    // 단, 스케쥴러에 의한 자동 배정의 경우를 고려하여 nullable 필드로 유지합니다.
    private Long decidedMemberId;

    // createdAt, updatedAt과는 중복되는 경향이 있으나 마지막으로 결정된 순간을 기록한다는 로깅 필드로서의 역할을 수행합니다.
    private Instant decidedAt;

    // 프로젝트 멤버의 상태를 나타냅니다. 추후 과거 참여 인원 등을 표시해주는 등의 확장성을 고려합니다.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMemberStatus status = ProjectMemberStatus.ACTIVE;
    private Instant statusUpdatedAt;
    private String statusChangeReason;
    private Long statusChangedMemberId;

    /**
     * 활성 상태의 신규 멤버를 생성합니다.
     */
    public static ProjectMember create(Project project, Long memberId, ChallengerPart part, Long decidedByMemberId) {
        ProjectMember pm = new ProjectMember();
        pm.project = project;
        pm.memberId = memberId;
        pm.part = part;
        pm.status = ProjectMemberStatus.ACTIVE;
        pm.decidedMemberId = decidedByMemberId;
        pm.decidedAt = Instant.now();
        return pm;
    }

    /**
     * 지원서 합격 결과로 활성 상태의 신규 멤버를 생성합니다.
     */
    public static ProjectMember createFromApplication(
        ProjectApplication application, ChallengerPart part, Long decidedByMemberId
    ) {
        ProjectMember pm = create(
            application.getApplicationForm().getProject(),
            application.getApplicantMemberId(),
            part,
            decidedByMemberId
        );
        pm.application = application;
        return pm;
    }

    /**
     * 멤버의 상태를 변경합니다 (soft delete 포함).
     * <p>
     * status 를 {@code newStatus} 로 바꾸고 변경 사유·주체·시각 메타데이터를 함께 기록합니다.
     * 행을 물리적으로 삭제하지 않으므로 히스토리가 보존됩니다.
     */
    public void changeStatus(ProjectMemberStatus newStatus, Long changedByMemberId, String reason) {
        this.status = newStatus;
        this.statusUpdatedAt = Instant.now();
        this.statusChangeReason = reason;
        this.statusChangedMemberId = changedByMemberId;
    }

    /**
     * 멤버를 강제 퇴출 처리합니다 (soft delete).
     * status 를 {@link ProjectMemberStatus#DISMISSED} 로 바꾸고 변경 메타데이터를 기록합니다.
     */
    public void dismiss(String reason, Long removedByMemberId) {
        changeStatus(ProjectMemberStatus.DISMISSED, removedByMemberId, reason);
    }

    /**
     * 멤버 활동을 자진 종료 처리합니다 (soft delete).
     * status 를 {@link ProjectMemberStatus#WITHDRAWN} 으로 바꾸고 변경 메타데이터를 기록합니다.
     * <p>
     * 프로젝트 중단(abort) 시 일괄 처리에도 동일 메서드를 사용하며, 사유에 명시합니다.
     */
    public void withdraw(String reason, Long decidedByMemberId) {
        changeStatus(ProjectMemberStatus.WITHDRAWN, decidedByMemberId, reason);
    }

    /**
     * 멤버 활동을 정상 완료 처리합니다.
     * status 를 {@link ProjectMemberStatus#COMPLETED} 로 바꾸고 변경 메타데이터를 기록합니다.
     * <p>
     * 프로젝트 완료(complete) 시 ACTIVE 멤버 일괄 처리에 사용합니다.
     */
    public void complete(Long decidedByMemberId) {
        changeStatus(ProjectMemberStatus.COMPLETED, decidedByMemberId, null);
    }
}
