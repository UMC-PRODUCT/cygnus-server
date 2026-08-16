package com.umc.product.form.adapter.out.persistence;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.product.form.domain.FormResponse;
import com.umc.product.form.domain.QFormResponse;
import com.umc.product.form.domain.enums.FormResponseStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FormResponseQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 폼의 모든 응답을 id 내림차순으로 조회 (DRAFT + SUBMITTED 모두 포함).
     */
    public List<FormResponse> findAllByFormId(Long formId) {
        QFormResponse fr = QFormResponse.formResponse;
        return queryFactory
            .selectFrom(fr)
            .where(fr.form.id.eq(formId))
            .orderBy(fr.id.desc())
            .fetch();
    }

    /**
     * 폼의 SUBMITTED 응답 목록을 id 내림차순으로 조회.
     */
    public List<FormResponse> findAllSubmittedByFormId(Long formId) {
        QFormResponse fr = QFormResponse.formResponse;
        return queryFactory
            .selectFrom(fr)
            .where(
                fr.form.id.eq(formId),
                fr.status.eq(FormResponseStatus.SUBMITTED)
            )
            .orderBy(fr.id.desc())
            .fetch();
    }

    /**
     * 여러 응답을 Form과 함께 조회한다.
     */
    public List<FormResponse> findAllByIdInWithForm(Set<Long> formResponseIds) {
        if (formResponseIds == null || formResponseIds.isEmpty()) {
            return List.of();
        }

        QFormResponse fr = QFormResponse.formResponse;
        return queryFactory
            .selectFrom(fr)
            .join(fr.form).fetchJoin()
            .where(fr.id.in(formResponseIds))
            .orderBy(fr.id.asc())
            .fetch();
    }
}
