package com.umc.product.authorization.domain;

import java.util.Set;

import com.umc.product.authorization.domain.exception.AuthorizationDomainException;
import com.umc.product.authorization.domain.exception.AuthorizationErrorCode;

import lombok.Getter;

/**
 * 권한 체크 대상이 되는 리소스 타입
 */
@Getter
public enum ResourceType {

    AUDIT("audit", "감사",
        Set.of(PermissionType.READ)),

    // Schedule 도메인
    SCHEDULE("schedule", "일정",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT,
            PermissionType.DELETE, PermissionType.FORCE_DELETE)),
    ATTENDANCE("attendance", "출석",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.APPROVE)),

    // Schedule 도메인 - 미사용
    ATTENDANCE_SHEET("attendance_sheet", "출석부",
        Set.of(PermissionType.APPROVE)),
    ATTENDANCE_RECORD("attendance_record", "출석 기록",
        Set.of(PermissionType.READ, PermissionType.APPROVE)),

    // 공지사항 관련
    NOTICE("notice", "공지사항",
        // WRITE는 Service 단에서 처리함
        Set.of(PermissionType.READ, PermissionType.EDIT, PermissionType.DELETE, PermissionType.CHECK)),

    // 커리큘럼 관련
    CURRICULUM("curriculum", "커리큘럼",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.DELETE)),
    WORKBOOK_SUBMISSION("workbook_submission", "워크북 제출 현황",
        Set.of(PermissionType.READ)),
    ORIGINAL_WORKBOOK("original_workbook", "원본 워크북",
        Set.of(PermissionType.MANAGE, PermissionType.RELEASE)),
    CHALLENGER_WORKBOOK("challenger_workbook", "챌린저 워크북",
        Set.of(PermissionType.WRITE, PermissionType.DELETE)),
    WEEKLY_BEST_WORKBOOK("weekly_best_workbook", "주간 베스트 워크북",
        Set.of(PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    MISSION_SUBMISSION("mission_submission", "미션 제출물",
        Set.of(PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    MISSION_FEEDBACK("mission_feedback", "미션 피드백",
        Set.of(PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),

    // 기수 - 지부 - 학교
    GISU("gisu", "기수",
        Set.of(PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    CHAPTER("chapter", "지부",
        Set.of(PermissionType.WRITE, PermissionType.DELETE)),
    SCHOOL("school", "학교",
        Set.of(PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),

    // 스터디 그룹 관련
    STUDY_GROUP("study_group", "스터디 그룹",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),

    // 커뮤니티 도메인
    COMMUNITY_POST("community_post", "커뮤니티 게시글",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    COMMUNITY_COMMENT("community_comment", "커뮤니티 게시글에 대한 댓글",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),

    // 블로그 도메인
    BLOG_CONTENT("blog_content", "블로그 콘텐츠",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    BLOG_SERIES("blog_series", "블로그 시리즈",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    BLOG_COMMENT("blog_comment", "블로그 댓글",
        Set.of(PermissionType.EDIT, PermissionType.DELETE)),

    // UMC 모집 관련
    RECRUITMENT("recruitment", "모집",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE,
            PermissionType.APPROVE, PermissionType.MANAGE)),

    // 회원 관련
    MEMBER("member", "회원",
        Set.of(PermissionType.READ, PermissionType.DELETE)),
    ANALYTICS("analytics", "운영진 대시보드",
        Set.of(PermissionType.READ)),
    TERM("term", "약관",
        Set.of(PermissionType.WRITE)),

    // 챌린저 관련
    CHALLENGER("challenger", "챌린저",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    CHALLENGER_ROLE("challenger_role", "운영진 기록",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),

    CHALLENGER_POINT("challenger_point", "챌린저 상벌점",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    CHALLENGER_RECORD("challenger_record", "챌린저 코드",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.DELETE)),

    // Notification
    FCM("fcm", "FCM 알람 관련",
        Set.of(PermissionType.WRITE, PermissionType.DELETE)),

    // UPMS, 프로젝트 관련

    PROJECT("project", "프로젝트",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT,
            PermissionType.DELETE, PermissionType.MANAGE)),
    // 프로젝트 지원서 관련
    PROJECT_APPLICATION("project_application", "프로젝트 지원서",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT,
            PermissionType.DELETE, PermissionType.APPROVE)),

    // 문의사항 관련
    INQUIRY("inquiry", "문의사항",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.EDIT, PermissionType.DELETE)),
    INQUIRY_MESSAGE("inquiry_message", "문의사항 메시지",
        Set.of(PermissionType.READ, PermissionType.WRITE, PermissionType.DELETE)),
    ;

    private final String code;
    private final String description;
    private final Set<PermissionType> supportedPermissions;

    ResourceType(String code, String description, Set<PermissionType> supportedPermissions) {
        this.code = code;
        this.description = description;
        this.supportedPermissions = Set.copyOf(supportedPermissions); // immutable로 보호
    }

    /**
     * code로 ResourceType 찾기
     */
    public static ResourceType fromCode(String code) {
        for (ResourceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + code);
    }

    /**
     * 해당 리소스 타입이 특정 권한을 지원하는지 확인
     */
    public boolean supports(PermissionType permission) {
        return supportedPermissions.contains(permission);
    }

    /**
     * 지원하지 않는 권한이면 예외를 발생시킴
     */
    public void validatePermission(PermissionType permission) {
        if (!supports(permission)) {
            String errorMessage =
                String.format("리소스 타입 '%s'은(는) '%s' 권한을 지원하지 않습니다. 지원 권한: %s",
                    this.name(), permission, supportedPermissions);

            throw new AuthorizationDomainException(
                AuthorizationErrorCode.PERMISSION_TYPE_NOT_SUPPORTED_BY_RESOURCE_TYPE,
                errorMessage);
        }
    }
}
