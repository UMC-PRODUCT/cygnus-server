package com.umc.product.feedback.domain;

import com.umc.product.common.BaseEntity;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_feedback_template")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFeedbackTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserFeedbackContext context;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserFeedbackTargetType targetType;

    @Column(nullable = false)
    private Long formId; // Form 도메인 Form ID 참조

    @Column(nullable = false)
    private boolean isActive;

    public static UserFeedbackTemplate create(UserFeedbackContext context, UserFeedbackTargetType targetType, Long formId) {
        return UserFeedbackTemplate.builder()
            .context(context)
            .targetType(targetType)
            .formId(formId)
            .isActive(true)
            .build();
    }
}
