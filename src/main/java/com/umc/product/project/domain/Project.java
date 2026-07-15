package com.umc.product.project.domain;

import java.util.Objects;

import com.umc.product.common.BaseEntity;
import com.umc.product.project.domain.enums.ProjectStatus;
import com.umc.product.project.domain.exception.ProjectDomainException;
import com.umc.product.project.domain.exception.ProjectErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long gisuId; // 프로젝트가 속한 기수

    @Column(nullable = false)
    private Long chapterId; // 프로젝트가 속한 지부

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;
    // ABORTED로 status를 변경하고자 하는 경우 반드시 ProjectMember 또한 변경하여야 합니다.

    // DRAFT 단계에서는 미입력 상태가 허용되므로 nullable.
    @Column(length = 100)
    private String name; // 프로젝트명

    @Column(length = 200)
    private String description; // 프로젝트 설명

    // storage 도메인 file_metadata.id는 UUID(VARCHAR)라 String으로 저장합니다.
    private String logoFileId; // 로고 파일 ID (UUID)
    private String thumbnailFileId; // 프로젝트 썸네일 이미지 파일 ID (UUID)


    @Column(length = 300)
    private String externalLink; // 프로젝트 외부 링크

    // 프로젝트를 생성한, PO의 ID 값입니다.
    // Plan 챌린저가 여러 명 있더라도 해당 프로젝트에 대한 전권을 가지고 있는 PO는 한 명으로 유지하고자 합니다.
    @Column(nullable = false)
    private Long productOwnerMemberId;

    /**
     * 메인 PM 의 학교 ID 비정규화 사본.
     * <p>
     * 학교 단위 scope/필터에서 단순 SQL 로 풀어내기 위함. Project 자체가 학교에 소속된 게 아니라
     * 메인 PM 의 학교를 캐시하는 의미. 양도 시점에 Service 가 새 PM 의 학교로 동기화한다.
     */
    @Column(nullable = false, name = "product_owner_school_id")
    private Long productOwnerSchoolId;

    /**
     * 프로젝트를 처음 생성한 멤버 ID. 운영진이 다른 PLAN 챌린저를 PO 로 지정해 만든 경우
     * {@code productOwnerMemberId} 와 다를 수 있다. DRAFT 단계 EDIT 권한 분기 + audit 용도.
     */
    @Column(nullable = false, name = "created_by_member_id")
    private Long creatorMemberId;

    private Long statusChangedByMemberId; // 프로젝트 상태를 변경한 멤버의 ID입니다. (예: ABORTED로 변경한 멤버)
    private String statusChangedReason; // 프로젝트 상태를 변경한 이유입니다. (예: ABORTED로 변경한 이유)

    // 프로젝트 지원 폼은, 파트별로 섹션을 나누어서 가지고 있어야 합니다.
    // Design, FE, BE 각각 섹션을 나누어서 가지고 있으며, 지원자의 파트에 따라서 자동으로 해당 섹션으로 렌더링해서 제공하여야 합니다.
    // 기존에는 ElementCollection, CollectionTable을 통해서 저장했으나, FormSection마다 조회 및 작성 가능한 파트를 제한하여야 하고,
    // 추후 요구사항에 보다 유연하게 대응하기 위해서 한정적인 매핑 테이블이 아닌 별도의 엔티티로 분리하였습니다.

    // 고려사항:
    // TO는 Embedded로 전부 때려박아서 관리할지, 별도의 Entity를 분리할지 고려하였으나
    // 추후 확장성을 고려하여 분리하는 방향으로 결정하였습니다.
    // (다음 기수부터 Web-Server 통합 가능성 고려)

    @Builder(access = AccessLevel.PRIVATE)
    private Project(
        Long gisuId,
        Long chapterId,
        ProjectStatus status,
        String name,
        String description,
        String externalLink,
        String logoFileId,
        String thumbnailFileId,
        Long productOwnerMemberId,
        Long productOwnerSchoolId,
        Long creatorMemberId
    ) {
        this.gisuId = gisuId;
        this.chapterId = chapterId;
        this.status = status;
        this.name = name;
        this.description = description;
        this.externalLink = externalLink;
        this.logoFileId = logoFileId;
        this.thumbnailFileId = thumbnailFileId;
        this.productOwnerMemberId = productOwnerMemberId;
        this.productOwnerSchoolId = productOwnerSchoolId;
        this.creatorMemberId = creatorMemberId;
    }

    /**
     * DRAFT 상태로 프로젝트를 생성합니다. 최초 생성 시 기수/지부/PO/creator 만 확정하고,
     * 나머지 정보(name, description 등)는 이후 updateBasicInfo로 단계별 저장합니다.
     *
     * @param gisuId               프로젝트가 속한 기수 ID
     * @param chapterId            프로젝트가 속한 지부 ID (PO의 소속 지부에서 결정)
     * @param productOwnerMemberId PO Member ID (PLAN 파트 챌린저여야 함)
     * @param productOwnerSchoolId PO 의 학교 ID 비정규화 (학교 운영진 scope 및 학교 필터에서 사용)
     * @param creatorMemberId      호출자(생성자) Member ID. PM 본인 등록 시엔 PO 와 동일,
     *                             운영진이 다른 챌린저를 PO 로 지정한 경우엔 다름.
     * @return DRAFT 상태의 프로젝트
     */
    public static Project createDraft(
        Long gisuId,
        Long chapterId,
        Long productOwnerMemberId,
        Long productOwnerSchoolId,
        Long creatorMemberId
    ) {
        return Project.builder()
            .gisuId(gisuId)
            .chapterId(chapterId)
            .productOwnerMemberId(productOwnerMemberId)
            .productOwnerSchoolId(productOwnerSchoolId)
            .creatorMemberId(creatorMemberId)
            .status(ProjectStatus.DRAFT)
            .build();
    }

    /**
     * 프로젝트 기본 정보를 부분 업데이트합니다. null 필드는 변경하지 않습니다.
     * 소유권(productOwnerMemberId)은 별도 액션({@link #transferOwnership})으로 처리합니다.
     */
    public void updateBasicInfo(
        String name,
        String description,
        String externalLink,
        String thumbnailFileId,
        String logoFileId
    ) {
        validateMutable();
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (externalLink != null) {
            this.externalLink = externalLink;
        }
        if (thumbnailFileId != null) {
            this.thumbnailFileId = thumbnailFileId;
        }
        if (logoFileId != null) {
            this.logoFileId = logoFileId;
        }
    }

    /**
     * 메인 PM(소유권)을 새 멤버에게 양도합니다. 종료 상태에서는 호출 불가.
     * <p>
     * 새 owner가 PLAN 파트인지, 동일 기수 내 다른 프로젝트가 없는지 등의 검증은
     * Service 레벨에서 수행합니다 (도메인은 다른 도메인 정보를 알 수 없음).
     * <p>
     * {@code productOwnerSchoolId} 와 {@code chapterId} 도 새 PM 기준으로 동기화한다.
     * 새 owner 가 다른 지부 소속이면 프로젝트도 새 지부로 이동한다 (권한/scope 정합성).
     */
    public void transferOwnership(Long newOwnerMemberId, Long newOwnerSchoolId, Long newChapterId) {
        validateMutable();
        this.productOwnerMemberId = newOwnerMemberId;
        this.productOwnerSchoolId = newOwnerSchoolId;
        this.chapterId = newChapterId;
    }

    /**
     * 전달된 멤버가 프로젝트의 메인 PM인지 확인합니다.
     */
    public boolean isOwner(Long memberId) {
        return Objects.equals(this.productOwnerMemberId, memberId);
    }

    /**
     * 프로젝트 본체(이름·소개·소유권 등)를 변경 가능한 상태인지 검증한다.
     * 종료 상태({@code COMPLETED}, {@code ABORTED})에서는 변경 불가.
     */
    public void validateMutable() {
        if (this.status == ProjectStatus.COMPLETED || this.status == ProjectStatus.ABORTED) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_INVALID_STATE);
        }
    }

    /**
     * 지원 폼을 편집 가능한 상태인지 검증한다.
     * <p>
     * - {@code DRAFT} / {@code PENDING_REVIEW}: 항상 허용.
     * - {@code IN_PROGRESS}: 활성 매칭 차수가 없을 때(차수 사이)만 허용. 차수 진행 중에는 금지.
     * - 그 외 상태: 금지.
     *
     * @param hasActiveRound 현재 시점에 활성화된 매칭 차수 존재 여부
     */
    public void validateApplicationFormEditable(boolean hasActiveRound) {
        if (this.status == ProjectStatus.DRAFT || this.status == ProjectStatus.PENDING_REVIEW) return;
        if (this.status == ProjectStatus.IN_PROGRESS && !hasActiveRound) return;
        throw new ProjectDomainException(ProjectErrorCode.PROJECT_INVALID_STATE);
    }

    /**
     * 챌린저가 본 프로젝트에 지원 가능한 상태인지 검증한다.
     * 모집 단계({@link ProjectStatus#IN_PROGRESS})에서만 허용한다.
     * 권한 레이어({@code ProjectApplicationPermissionEvaluator}) 와 분리된 도메인 가드 — 스케줄러 등 권한 우회 경로에서도 invariant 보호.
     */
    public void validateApplicable() {
        if (this.status != ProjectStatus.IN_PROGRESS) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_INVALID_STATE);
        }
    }

    /**
     * DRAFT 상태에서 PENDING_REVIEW 상태로 전이합니다. PM 제출 액션.
     * - 현재 상태가 DRAFT가 아니면 {@code PROJECT_INVALID_STATE}.
     * - {@code name} 미입력 시 {@code PROJECT_SUBMIT_VALIDATION_FAILED}.
     * <p>
     * 지원 폼({@link ProjectApplicationForm}) 연결 여부는 다른 도메인 lookup이 필요하므로
     * Service 레이어에서 {@code submit()} 호출 전 검증합니다.
     */
    public void submit() {
        validateStatus(ProjectStatus.DRAFT);
        validateSubmitRequiredFields();
        this.status = ProjectStatus.PENDING_REVIEW;
    }

    /**
     * Admin 검토 후 프로젝트를 공개 상태로 전이한다 (PENDING_REVIEW → IN_PROGRESS).
     * <p>
     * 파트별 TO/지원 폼 등 외부 도메인 의존 검증은 Service 레이어에서 호출 전 수행한다.
     */
    public void publish() {
        validateStatus(ProjectStatus.PENDING_REVIEW);
        this.status = ProjectStatus.IN_PROGRESS;
    }

    /**
     * 기수가 종료되었을 때, 프로젝트를 완료 처리 합니다. IN_PROGRESS 상태에서만 허용합니다.
     *
     * @param decidedByMemberId 완료 처리한 운영진 Member ID (audit 용)
     */
    public void complete(Long decidedByMemberId) {
        validateStatus(ProjectStatus.IN_PROGRESS);
        this.status = ProjectStatus.COMPLETED;
        this.statusChangedByMemberId = decidedByMemberId;
    }

    /**
     * 프로젝트 hard delete 가능 상태인지 검증한다. DRAFT/PENDING_REVIEW 단계에서만 허용.
     * IN_PROGRESS 이상은 모집/매칭 데이터가 누적되므로 {@link #abort} 로 상태 전이해야 한다.
     */
    public void validateDeletable() {
        if (this.status != ProjectStatus.DRAFT && this.status != ProjectStatus.PENDING_REVIEW) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_DELETE_NOT_ALLOWED_IN_STATUS);
        }
    }

    /**
     * 프로젝트가 중간에 와해되었을 때 사용합니다. 사유를 반드시 제공하여야 합니다.
     *
     * @param reason            와해 사유
     * @param decidedByMemberId 해당 사항을 결정한 운영진 Member ID
     */
    public void abort(String reason, Long decidedByMemberId) {
        if (this.status == ProjectStatus.COMPLETED || this.status == ProjectStatus.ABORTED) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_ABORT_UNAVAILABLE);
        }
        this.status = ProjectStatus.ABORTED;
        this.statusChangedReason = reason;
        this.statusChangedByMemberId = decidedByMemberId;
    }

    private void validateStatus(ProjectStatus expected) {
        if (this.status != expected) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_INVALID_STATE);
        }
    }

    private void validateSubmitRequiredFields() {
        if (this.name == null || this.name.isBlank()) {
            throw new ProjectDomainException(ProjectErrorCode.PROJECT_SUBMIT_VALIDATION_FAILED);
        }
    }

}
