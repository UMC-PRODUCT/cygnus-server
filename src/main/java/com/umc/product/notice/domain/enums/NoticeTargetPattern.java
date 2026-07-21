package com.umc.product.notice.domain.enums;

import com.umc.product.authorization.application.port.in.query.GetChallengerRoleUseCase;
import com.umc.product.notice.domain.NoticeTargetInfo;
import com.umc.product.notice.domain.exception.NoticeDomainException;
import com.umc.product.notice.domain.exception.NoticeErrorCode;

public enum NoticeTargetPattern {

    // =============================
    // 챌린저 공지 - 전체 기수 대상
    // =============================

    ALL_GISU_ALL_TARGET(false, false, false, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isCentralCoreInAnyGisu(memberId);
        }
    },

    ALL_GISU_SPECIFIC_SCHOOL(false, false, true, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isSchoolCoreInAnyGisu(memberId, info.targetSchoolId());
        }
    },

    // (오류) 전체 기수 특정 학교 특정 파트
    ALL_GISU_SPECIFIC_SCHOOL_WITH_PART(false, false, true, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                "전체 기수를 대상으로 하는 경우, 교내 특정 파트를 한정한 공지는 불가능합니다.");
        }
    },

    // (오류) 전체 기수 특정 지부
    ALL_GISU_WITH_CHAPTER(false, true, false, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                "기수가 주어지지 않은 상태에서 지부 대상으로 공지를 작성할 수 없습니다.");
        }
    },

    // =============================
    // 챌린저 공지 - 특정 기수 대상
    // =============================

    SPECIFIC_GISU_ALL_TARGET(true, false, false, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            // 특정 기수 전체공지는 해당 기수의 중앙운영진(총괄단 + 운영국원 + 교육국원) 모두 작성 가능
            return useCase.isCentralMemberInGisu(memberId, info.targetGisuId());
        }
    },

    SPECIFIC_GISU_SPECIFIC_PART(true, false, false, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isCentralMemberInGisu(memberId, info.targetGisuId());
        }
    },

    SPECIFIC_GISU_SPECIFIC_SCHOOL(true, false, true, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isSchoolCoreInGisu(memberId, info.targetGisuId(), info.targetSchoolId());
        }
    },

    SPECIFIC_GISU_SPECIFIC_SCHOOL_WITH_PART(true, false, true, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isSchoolAdminInGisu(memberId, info.targetGisuId(), info.targetSchoolId());
        }
    },

    SPECIFIC_GISU_SPECIFIC_CHAPTER(true, true, false, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isChapterPresidentInGisu(memberId, info.targetGisuId(), info.targetChapterId());
        }
    },

    SPECIFIC_GISU_SPECIFIC_CHAPTER_WITH_PART(true, true, false, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return useCase.isChapterPresidentInGisu(memberId, info.targetGisuId(), info.targetChapterId());
        }
    },

    // (오류) 특정 기수 특정 지부 특정 학교
    INVALID_GISU_CHAPTER_SCHOOL(true, true, true, false) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                "기수, 지부, 학교는 동시에 지정할 수 없습니다.");
        }
    },

    // =============================
    // 운영진 공지 - 특정 기수 대상
    // =============================

    // 특정 기수 전체 운영진 (schoolId=null, part=null)
    STAFF_SPECIFIC_GISU(true, false, false, false, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            return hasStaffWritePermission(info, memberId, useCase);
        }
    },

    // 특정 기수 특정 파트 운영진 (schoolId=null, part 지정): 중앙운영진 → 파트장 공지
    STAFF_SPECIFIC_GISU_SPECIFIC_PART(true, false, false, true, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            validatePartForStaff(info);
            return hasStaffWritePermission(info, memberId, useCase);
        }
    },

    // 특정 기수 특정 학교 운영진 (schoolId 지정, part 있음/없음 모두): 교내운영진 공지
    STAFF_SPECIFIC_GISU_SPECIFIC_SCHOOL(true, false, true, false, true) {
        @Override
        public boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase) {
            validateSchoolForCentralRole(info);
            return hasStaffWritePermission(info, memberId, useCase);
        }
    };

    private final boolean hasGisu;
    private final boolean hasChapter;
    private final boolean hasSchool;
    private final boolean hasPart;
    private final boolean hasStaffRoles;

    NoticeTargetPattern(boolean hasGisu, boolean hasChapter, boolean hasSchool, boolean hasPart) {
        this(hasGisu, hasChapter, hasSchool, hasPart, false);
    }

    NoticeTargetPattern(boolean hasGisu, boolean hasChapter, boolean hasSchool, boolean hasPart,
                        boolean hasStaffRoles) {
        this.hasGisu = hasGisu;
        this.hasChapter = hasChapter;
        this.hasSchool = hasSchool;
        this.hasPart = hasPart;
        this.hasStaffRoles = hasStaffRoles;
    }

    public static NoticeTargetPattern from(NoticeTargetInfo info) {
        boolean hasGisu = info.targetGisuId() != null;
        boolean hasChapter = info.targetChapterId() != null;
        boolean hasSchool = info.targetSchoolId() != null;
        boolean hasPart = info.targetParts() != null && !info.targetParts().isEmpty();
        boolean isStaff = info.isStaffNotice();

        if (isStaff) {
            if (!hasGisu) {
                throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                    "운영진 공지는 기수 지정이 필수입니다.");
            }
            if (hasChapter) {
                throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                    "운영진 공지에 지부를 지정할 수 없습니다.");
            }
            // 교내운영진 공지 (schoolId 지정): part 유무와 무관하게 동일 패턴
            if (hasSchool) {
                return STAFF_SPECIFIC_GISU_SPECIFIC_SCHOOL;
            }
            if (hasPart) {
                return STAFF_SPECIFIC_GISU_SPECIFIC_PART;
            }
            return STAFF_SPECIFIC_GISU;
        }

        for (NoticeTargetPattern pattern : values()) {
            if (pattern.matches(hasGisu, hasChapter, hasSchool, hasPart, false)) {
                return pattern;
            }
        }

        throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
            "지원하지 않는 대상 설정입니다. hasGisu: " + hasGisu + ", hasChapter: " + hasChapter
                + ", hasSchool: " + hasSchool + ", hasPart: " + hasPart);
    }

    // CENTRAL_MEMBER 대상 공지에는 학교 지정 불가
    private static void validateSchoolForCentralRole(NoticeTargetInfo info) {
        if (info.targetNoticeTab() == NoticeTab.CENTRAL_MEMBER) {
            throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                "중앙운영진을 대상으로 하는 공지에 학교를 지정할 수 없습니다.");
        }
    }

    // SCHOOL_CORE 대상 공지에는 파트 지정 불가 (파트 지정은 SCHOOL_PART_LEADER/CENTRAL_MEMBER만 허용)
    private static void validatePartForStaff(NoticeTargetInfo info) {
        if (info.targetNoticeTab() == NoticeTab.SCHOOL_CORE) {
            throw new NoticeDomainException(NoticeErrorCode.INVALID_TARGET_SETTING,
                "교내 회장단 대상 공지에는 파트를 지정할 수 없습니다.");
        }
    }

    private static boolean hasStaffWritePermission(NoticeTargetInfo info, Long memberId,
                                                   GetChallengerRoleUseCase useCase) {
        if (info.targetNoticeTab() == NoticeTab.CENTRAL_MEMBER) {
            // 중앙운영진 대상 공지: 총괄단만 작성 가능
            return useCase.isCentralCoreInGisu(memberId, info.targetGisuId());
        }
        if (info.targetSchoolId() != null) {
            // 교내운영진 공지 (SCHOOL_PART_LEADER + schoolId): 해당 학교 회장단만 작성 가능
            return useCase.isSchoolCoreInGisu(memberId, info.targetGisuId(), info.targetSchoolId());
        }
        // 학교 운영진 대상 중앙 공지 (SCHOOL_CORE / SCHOOL_PART_LEADER + schoolId=null): 중앙운영진 작성 가능
        return useCase.isCentralMemberInGisu(memberId, info.targetGisuId());
    }

    public abstract boolean validatePermission(NoticeTargetInfo info, Long memberId, GetChallengerRoleUseCase useCase);

    private boolean matches(boolean hasGisu, boolean hasChapter, boolean hasSchool, boolean hasPart,
                            boolean hasStaffRoles) {
        return (this.hasGisu == hasGisu)
            && (this.hasChapter == hasChapter)
            && (this.hasSchool == hasSchool)
            && (this.hasPart == hasPart)
            && (this.hasStaffRoles == hasStaffRoles);
    }
}
