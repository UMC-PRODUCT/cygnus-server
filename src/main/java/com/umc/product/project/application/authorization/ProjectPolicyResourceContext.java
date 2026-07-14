package com.umc.product.project.application.authorization;

import java.util.Objects;
import java.util.Optional;

import com.umc.product.project.domain.enums.ProjectApplicationStatus;
import com.umc.product.project.domain.enums.ProjectStatus;

public record ProjectPolicyResourceContext(
    Optional<Long> projectId,
    Optional<Long> gisuId,
    Optional<Long> chapterId,
    Optional<ProjectStatus> projectStatus,
    Optional<Long> applicationId,
    Optional<ProjectApplicationStatus> applicationStatus,
    Optional<Long> matchingRoundId,
    Optional<Long> creatorMemberId,
    Optional<Long> productOwnerMemberId,
    Optional<Long> applicantMemberId,
    boolean activePlanMember,
    boolean requesterHasOwnedProjectInResourceGisu,
    boolean superAdminAllowDraftRead
) {
    public ProjectPolicyResourceContext {
        projectId = Objects.requireNonNull(projectId);
        gisuId = Objects.requireNonNull(gisuId);
        chapterId = Objects.requireNonNull(chapterId);
        projectStatus = Objects.requireNonNull(projectStatus);
        applicationId = Objects.requireNonNull(applicationId);
        applicationStatus = Objects.requireNonNull(applicationStatus);
        matchingRoundId = Objects.requireNonNull(matchingRoundId);
        creatorMemberId = Objects.requireNonNull(creatorMemberId);
        productOwnerMemberId = Objects.requireNonNull(productOwnerMemberId);
        applicantMemberId = Objects.requireNonNull(applicantMemberId);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Long projectId;
        private Long gisuId;
        private Long chapterId;
        private ProjectStatus projectStatus;
        private Long applicationId;
        private ProjectApplicationStatus applicationStatus;
        private Long matchingRoundId;
        private Long creatorMemberId;
        private Long productOwnerMemberId;
        private Long applicantMemberId;
        private boolean activePlanMember;
        private boolean requesterHasOwnedProjectInResourceGisu;
        private boolean superAdminAllowDraftRead;

        private Builder() {}

        public Builder project(long id, long resourceGisuId, long resourceChapterId, ProjectStatus status) {
            projectId = id;
            gisuId = resourceGisuId;
            chapterId = resourceChapterId;
            projectStatus = status;
            return this;
        }

        public Builder projectTarget(long resourceGisuId, long resourceChapterId) {
            gisuId = resourceGisuId;
            chapterId = resourceChapterId;
            return this;
        }

        public Builder gisuScope(long resourceGisuId) {
            gisuId = resourceGisuId;
            return this;
        }

        public Builder chapterScope(long resourceGisuId, long resourceChapterId) {
            gisuId = resourceGisuId;
            chapterId = resourceChapterId;
            return this;
        }

        public Builder application(long id, ProjectApplicationStatus status, long applicantId) {
            applicationId = id;
            applicationStatus = status;
            applicantMemberId = applicantId;
            return this;
        }

        public Builder matchingRound(Long id, long resourceGisuId, long resourceChapterId) {
            matchingRoundId = id;
            gisuId = resourceGisuId;
            chapterId = resourceChapterId;
            return this;
        }

        public Builder creatorMemberId(long memberId) {
            creatorMemberId = memberId;
            return this;
        }

        public Builder productOwnerMemberId(long memberId) {
            productOwnerMemberId = memberId;
            return this;
        }

        public Builder activePlanMember(boolean value) {
            activePlanMember = value;
            return this;
        }

        public Builder requesterHasOwnedProjectInResourceGisu(boolean value) {
            requesterHasOwnedProjectInResourceGisu = value;
            return this;
        }

        public Builder superAdminAllowDraftRead(boolean value) {
            superAdminAllowDraftRead = value;
            return this;
        }

        public ProjectPolicyResourceContext build() {
            return new ProjectPolicyResourceContext(
                Optional.ofNullable(projectId),
                Optional.ofNullable(gisuId),
                Optional.ofNullable(chapterId),
                Optional.ofNullable(projectStatus),
                Optional.ofNullable(applicationId),
                Optional.ofNullable(applicationStatus),
                Optional.ofNullable(matchingRoundId),
                Optional.ofNullable(creatorMemberId),
                Optional.ofNullable(productOwnerMemberId),
                Optional.ofNullable(applicantMemberId),
                activePlanMember,
                requesterHasOwnedProjectInResourceGisu,
                superAdminAllowDraftRead
            );
        }
    }
}
