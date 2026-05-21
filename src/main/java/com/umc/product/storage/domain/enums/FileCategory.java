package com.umc.product.storage.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 카테고리
 * <p>
 * 파일의 용도에 따라 저장 경로, 허용 확장자, 최대 크기 등을 다르게 관리할 수 있습니다.
 * <p>
 * 카테고리에 따라서 정책을 지속적으로 업데이트 하면 됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum FileCategory {
    /**
     * 프로필 이미지
     */
    PROFILE_IMAGE("public/profile", 5 * 1024 * 1024, new String[]{"jpg", "jpeg", "png", "webp"}),

    /**
     * 게시글 이미지
     */
    POST_IMAGE("public/post", 10 * 1024 * 1024, new String[]{"jpg", "jpeg", "png", "webp", "gif"}),

    /**
     * 게시글 첨부파일
     */
    POST_ATTACHMENT("public/attachment", 50 * 1024 * 1024,
        new String[]{"pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip"}),

    /**
     * 공지사항 첨부파일
     */
    NOTICE_ATTACHMENT("public/notice", 10 * 1024 * 1024,
        new String[]{"jpg", "jpeg", "png", "webp", "gif", "heic"}),

    /**
     * 워크북 제출 파일
     */
    WORKBOOK_SUBMISSION("public/workbook", 20 * 1024 * 1024, new String[]{"pdf", "jpg", "jpeg", "png", "zip"}),

    /**
     * 학교 로고 이미지
     */
    SCHOOL_LOGO("public/school-logo", 5 * 1024 * 1024, new String[]{"jpg", "jpeg", "png", "webp"}),

    /**
     * PDF 포트폴리오 파일; Web에서 지원서 작성 시 활용합니다.
     */
    PORTFOLIO("private/portfolio", 200 * 1024 * 1024, new String[]{"pdf"}),

    /**
     * 프로젝트 썸네일 이미지 (S-CHL-01 카드용 540x286)
     */
    PROJECT_THUMBNAIL("public/project-thumbnail", 10 * 1024 * 1024, new String[]{"jpg", "jpeg", "png"}),

    /**
     * 프로젝트 로고 이미지 (정사각형)
     */
    PROJECT_LOGO("public/project-logo", 10 * 1024 * 1024, new String[]{"jpg", "jpeg", "png", "svg"}),

    /**
     * 문의사항 첨부파일
     */
    INQUIRY_ATTACHMENT("public/inquiry", 20 * 1024 * 1024,
        new String[]{"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx"}),

    /**
     * 기타
     */
    ETC("public/etc", 10 * 1024 * 1024, new String[]{});

    /**
     * 저장 경로 prefix
     */
    private final String pathPrefix;

    /**
     * 최대 파일 크기 (bytes)
     */
    private final long maxSize;

    /**
     * 허용 확장자 목록 (빈 배열이면 모든 확장자 허용)
     */
    private final String[] allowedExtensions;

    /**
     * 확장자가 허용되는지 확인
     */
    public boolean isAllowedExtension(String extension) {
        if (allowedExtensions.length == 0) {
            return true;
        }

        String normalizedExtension = extension.toLowerCase();
        for (String allowed : allowedExtensions) {
            if (allowed.equals(normalizedExtension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 파일 크기가 허용 범위 내인지 확인
     */
    public boolean isAllowedSize(long fileSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }
}
