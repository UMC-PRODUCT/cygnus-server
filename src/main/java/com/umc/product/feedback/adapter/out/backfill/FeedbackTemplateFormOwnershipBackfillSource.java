package com.umc.product.feedback.adapter.out.backfill;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.feedback.application.policy.FeedbackTemplateOwnerReferenceFactory;
import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;

import lombok.RequiredArgsConstructor;

@Component
@Profile("registry-backfill")
@RequiredArgsConstructor
public class FeedbackTemplateFormOwnershipBackfillSource implements FormOwnershipBackfillSource {

    public static final String SOURCE_NAME = "feedback-template";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String namespace() {
        return FeedbackTemplateOwnerReferenceFactory.NAMESPACE;
    }

    @Override
    public String slot() {
        return FeedbackTemplateOwnerReferenceFactory.SLOT;
    }

    @Override
    public List<FormOwnershipBackfillMapping> loadBatch(long afterParentId, int batchSize) {
        return jdbcTemplate.query("""
            SELECT id AS parent_id, form_id, id::text AS owner_resource_key
            FROM user_feedback_template
            WHERE id > ?
            ORDER BY id
            LIMIT ?
            """, (resultSet, rowNum) -> new FormOwnershipBackfillMapping(
                resultSet.getLong("parent_id"),
                resultSet.getLong("form_id"),
                resultSet.getString("owner_resource_key")
            ), afterParentId, batchSize);
    }
}
