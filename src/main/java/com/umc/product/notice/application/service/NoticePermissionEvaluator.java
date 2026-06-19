package com.umc.product.notice.application.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.umc.product.authorization.application.port.out.ResourcePermissionEvaluator;
import com.umc.product.authorization.domain.AuthoritySnapshot;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.RoleAttribute;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.authorization.domain.SubjectAttributes.GisuChallengerInfo;
import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.notice.application.port.in.query.GetNoticeTargetUseCase;
import com.umc.product.notice.application.port.out.LoadNoticePort;
import com.umc.product.notice.domain.Notice;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.enums.NoticeTab;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Notice(공지사항) 리소스에 대한 권한 평가
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NoticePermissionEvaluator implements ResourcePermissionEvaluator {

    private final GetNoticeTargetUseCase getNoticeTargetUseCase;
    private final LoadNoticePort loadNoticePort;

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.NOTICE;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes,
                            ResourcePermission resourcePermission) {
        if (!resourcePermission.resourceType().getSupportedPermissions()
            .contains(resourcePermission.permission())) {
            throw new AuthorizationDomainException(AuthorizationErrorCode.INVALID_RESOURCE_PERMISSION_GIVEN,
                "NoticePermissionEvaluator에서 지원하지 않는 권한 유형에 대한 평가가 시도되었습니다: " + resourcePermission.permission());
        }

        // WRITE는 별도로 지원하지 않음, Service에서 직접 확인함

        NoticeTargetInfo targetInfo =
            getNoticeTargetUseCase.findByNoticeId(resourcePermission.getResourceIdAsLong());

        return switch (resourcePermission.permission()) {
            case READ -> canReadNotice(subjectAttributes, targetInfo);
            case EDIT, DELETE -> canDeleteOrEditNotice(subjectAttributes, resourcePermission);
            // TODO: Check는 임시로 Manage랑 동일하게 적용, 하나야 수정해줘!
            case MANAGE, CHECK -> canManageNotice(subjectAttributes, targetInfo);
            default -> throw new AuthorizationDomainException(AuthorizationErrorCode.PERMISSION_TYPE_NOT_IMPLEMENTED,
                "NoticePE에서 해당 PermissionType을 지원하지 않습니다: " + resourcePermission.permission());
        };
    }

    private boolean canReadNotice(SubjectAttributes subjectAttributes, NoticeTargetInfo targetInfo) {
        // 총괄/부총괄: 모든 공지 읽기 가능
        if (canReadAllAsCentralCore(subjectAttributes, targetInfo)) {
            return true;
        }

        if (targetInfo.isStaffNotice()) {
            return canReadStaffNotice(subjectAttributes, targetInfo);
        }

        // 기본 챌린저 권한 체크 (본인의 part, gisu, chapter 기반)
        for (GisuChallengerInfo gisuChallengerInfo : subjectAttributes.gisuChallengerInfos()) {
            if (targetInfo.isTarget(
                gisuChallengerInfo.gisuId(),
                gisuChallengerInfo.chapterId(),
                subjectAttributes.schoolId(),
                gisuChallengerInfo.part()
            )) {
                return true;
            }
        }

        // 역할 기반 추가 읽기 권한 체크
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(role -> canReadByRole(role, targetInfo, subjectAttributes));
    }

    private boolean canReadStaffNotice(SubjectAttributes subjectAttributes, NoticeTargetInfo targetInfo) {
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(role -> {
                NoticeTab viewerRole = NoticeTab.findFrom(role.roleType()).orElse(null);
                if (viewerRole == null) {
                    return false;
                }
                // 역할 레벨 확인: 공지의 minTargetRole이 viewerRole을 포함하는지 (하한선 체크)
                if (!targetInfo.targetNoticeTab().includes(viewerRole)) {
                    return false;
                }
                // 기수 범위 확인
                if (targetInfo.targetGisuId() != null && !targetInfo.targetGisuId().equals(role.gisuId())) {
                    return false;
                }
                // 학교 범위 확인 (교내운영진 공지)
                if (targetInfo.targetSchoolId() != null
                    && !targetInfo.targetSchoolId().equals(role.organizationId())) {
                    return false;
                }
                // 파트 범위 확인: 파트장만 담당 파트로 필터링 (회장단/중앙운영진은 파트 무관)
                // ADMIN 파트(기타 교내 운영진)는 파트 구분 없이 열람 가능
                if (viewerRole == NoticeTab.SCHOOL_PART_LEADER
                    && targetInfo.targetParts() != null && !targetInfo.targetParts().isEmpty()) {
                    boolean isPartFree = role.responsiblePart() == null
                        || role.responsiblePart() == ChallengerPart.ADMIN;
                    if (!isPartFree && !targetInfo.targetParts().contains(role.responsiblePart())) {
                        return false;
                    }
                }
                return true;
            });
    }

    /**
     * 역할에 따른 추가 읽기 권한 평가
     * <p>
     * 기본 챌린저 체크(part 기반)를 통과하지 못한 경우에만 호출되도록 구현
     */
    private boolean canReadByRole(RoleAttribute role, NoticeTargetInfo targetInfo, SubjectAttributes subject) {
        return switch (role.roleType()) {
            // 중앙운영진: 본인 기수 범위의 모든 챌린저 공지를 파트 무관하게 읽기 가능
            case CENTRAL_OPERATING_TEAM_MEMBER, CENTRAL_EDUCATION_TEAM_MEMBER -> targetInfo.targetGisuId() == null ||
                targetInfo.targetGisuId().equals(role.gisuId());
            case CHAPTER_PRESIDENT -> chapterPresidentCanRead(role, targetInfo, subject);
            case SCHOOL_PRESIDENT, SCHOOL_VICE_PRESIDENT -> schoolCoreCanRead(role, targetInfo, subject);
            case SCHOOL_PART_LEADER -> schoolPartLeaderCanRead(role, targetInfo, subject);
            default -> false;
        };
    }

    private boolean canDeleteOrEditNotice(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        if (subjectAttributes.toAuthoritySnapshot().isSuperAdmin()) {
            return true;
        }

        Notice notice = loadNoticePort.findNoticeById(resourcePermission.getResourceIdAsLong())
            .orElseThrow(() -> new NoticeDomainException(NoticeErrorCode.NOTICE_NOT_FOUND));

        return Objects.equals(subjectAttributes.memberId(), notice.getAuthorMemberId());
    }

    /**
     * 공지사항 관리 권한 확인 (수신 현황 조회 등) - 총괄/부총괄: 항상 허용 - School 레벨 공지: 해당 학교 운영진 - Chapter 레벨 공지: 해당 지부장 - Gisu 레벨 공지: 중앙 멤버
     */
    private boolean canManageNotice(SubjectAttributes subjectAttributes, NoticeTargetInfo targetInfo) {
        AuthoritySnapshot snapshot = subjectAttributes.toAuthoritySnapshot();
        if (targetInfo.targetGisuId() != null && snapshot.isCentralCoreInGisu(targetInfo.targetGisuId())) {
            return true;
        }
        if (targetInfo.targetGisuId() == null && snapshot.isCentralCoreInAnyGisu()) {
            return true;
        }

        if (targetInfo.targetSchoolId() != null) {
            if (targetInfo.targetGisuId() != null) {
                return snapshot.isSchoolAdminInGisu(targetInfo.targetGisuId(), targetInfo.targetSchoolId());
            }
            return snapshot.isSchoolAdminInAnyGisu(targetInfo.targetSchoolId());
        }

        if (targetInfo.targetChapterId() != null) {
            if (targetInfo.targetGisuId() != null) {
                return snapshot.isChapterPresidentInGisu(targetInfo.targetGisuId(), targetInfo.targetChapterId());
            }
            return snapshot.isChapterPresidentInAnyGisu(targetInfo.targetChapterId());
        }

        if (targetInfo.targetGisuId() != null) {
            return snapshot.isCentralMemberInGisu(targetInfo.targetGisuId());
        }
        return snapshot.isCentralMemberInAnyGisu();
    }

    private boolean canReadAllAsCentralCore(SubjectAttributes subjectAttributes, NoticeTargetInfo targetInfo) {
        AuthoritySnapshot snapshot = subjectAttributes.toAuthoritySnapshot();
        if (targetInfo.targetGisuId() != null) {
            return snapshot.isCentralCoreInGisu(targetInfo.targetGisuId());
        }
        return snapshot.isCentralCoreInAnyGisu();
    }

    private boolean chapterPresidentCanRead(RoleAttribute role, NoticeTargetInfo targetInfo,
                                            SubjectAttributes subject) {
        Long myChapterId = role.organizationId();
        if (myChapterId == null) {
            return false;
        }
        if (targetInfo.targetChapterId() != null && !myChapterId.equals(targetInfo.targetChapterId())) {
            return false;
        }
        if (targetInfo.targetSchoolId() != null
            && (subject.schoolId() == null || !subject.schoolId().equals(targetInfo.targetSchoolId()))) {
            return false;
        }
        if (targetInfo.targetChapterId() == null && targetInfo.targetSchoolId() == null) {
            return false;
        }
        return subject.gisuChallengerInfos().stream()
            .filter(info -> myChapterId.equals(info.chapterId()))
            .filter(info -> role.gisuId().equals(info.gisuId()))
            .anyMatch(info -> targetInfo.targetGisuId() == null
                || targetInfo.targetGisuId().equals(info.gisuId()));
    }

    private boolean schoolCoreCanRead(RoleAttribute role, NoticeTargetInfo targetInfo, SubjectAttributes subject) {
        Long mySchoolId = role.organizationId();
        if (mySchoolId == null) {
            return false;
        }
        // 학교 대상 공지는 본인 학교에 한정. 지부/전체 범위 공지(targetSchoolId == null)는
        // 기수·지부 일치 여부로만 판정하여 회장단이 본인 지부공지를 파트 무관하게 읽을 수 있도록 함.
        if (targetInfo.targetSchoolId() != null && !mySchoolId.equals(targetInfo.targetSchoolId())) {
            return false;
        }
        return isInGisuAndChapter(role, targetInfo, subject);
    }

    private boolean schoolPartLeaderCanRead(RoleAttribute role, NoticeTargetInfo targetInfo,
                                            SubjectAttributes subject) {
        ChallengerPart responsiblePart = role.responsiblePart();
        if (responsiblePart == null) {
            return false;
        }
        Long mySchoolId = role.organizationId();
        if (mySchoolId == null) {
            return false;
        }
        if (targetInfo.targetSchoolId() != null && !mySchoolId.equals(targetInfo.targetSchoolId())) {
            return false;
        }
        if (targetInfo.targetParts() == null || targetInfo.targetParts().isEmpty()
            || !targetInfo.targetParts().contains(responsiblePart)) {
            return false;
        }
        return isInGisuAndChapter(role, targetInfo, subject);
    }

    private boolean isInGisuAndChapter(RoleAttribute role, NoticeTargetInfo targetInfo, SubjectAttributes subject) {
        return subject.gisuChallengerInfos().stream()
            .filter(info -> role.gisuId().equals(info.gisuId()))
            .anyMatch(info ->
                (targetInfo.targetGisuId() == null || targetInfo.targetGisuId().equals(info.gisuId()))
                    && (targetInfo.targetChapterId() == null || targetInfo.targetChapterId().equals(info.chapterId())));
    }
}
