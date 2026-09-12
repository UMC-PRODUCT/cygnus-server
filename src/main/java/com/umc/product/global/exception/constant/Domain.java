package com.umc.product.global.exception.constant;

// Error와 함께 기입할 도메인 구분용 enum

/**
 * 의도에 의해 발생한 Buisioness Exception의 경우, 도메인을 구분하기 위해 사용됩니다.
 */
public enum Domain {
    COMMON,
    AUTHENTICATION,
    AUTHORIZATION,
    MEMBER,
    CHALLENGER,
    ORGANIZATION,
    CURRICULUM,
    SCHEDULE,
    COMMUNITY,
    NOTICE,
    FCM,
    FORM,
    RECRUITMENT,
    TERMS,
    EMAIL,
    STORAGE,
    WEBHOOK,
    AUDIT_LOG,
    PROJECT,
    LLM,
    ANALYTICS,
    INQUIRY,
    CHAT,
    MAINTENANCE,
    FEEDBACK,
    BLOG,
    CERTIFICATE,
    DEMODAY
}
