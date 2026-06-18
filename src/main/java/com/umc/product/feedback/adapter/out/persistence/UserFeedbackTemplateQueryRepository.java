package com.umc.product.feedback.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.feedback.domain.QUserFeedbackTemplate;
import com.umc.product.feedback.domain.UserFeedbackTemplate;
import com.umc.product.feedback.domain.enums.UserFeedbackContext;
import com.umc.product.feedback.domain.enums.UserFeedbackTargetType;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserFeedbackTemplateQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<UserFeedbackTemplate> findByCondition(
        UserFeedbackContext context,
        UserFeedbackTargetType targetType,
        Boolean active
    ) {
        QUserFeedbackTemplate template = QUserFeedbackTemplate.userFeedbackTemplate;
        BooleanBuilder condition = new BooleanBuilder();

        if (context != null) {
            condition.and(template.context.eq(context));
        }
        if (targetType != null) {
            condition.and(template.targetType.eq(targetType));
        }
        if (active != null) {
            condition.and(template.isActive.eq(active));
        }

        return queryFactory
            .selectFrom(template)
            .where(condition)
            .orderBy(template.updatedAt.desc(), template.id.desc())
            .fetch();
    }
}
