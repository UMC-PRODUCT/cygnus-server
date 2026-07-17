package com.umc.product.project.adapter.out.backfill;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.form.application.port.out.dto.FormOwnershipBackfillMapping;
import com.umc.product.project.application.form.ProjectApplicationFormOwnerReferenceFactory;

import lombok.RequiredArgsConstructor;

@Component
@Profile("registry-backfill")
@RequiredArgsConstructor
public class ProjectFormOwnershipBackfillSource implements FormOwnershipBackfillSource {

    public static final String SOURCE_NAME = "project-application-form";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String sourceName() {
        return SOURCE_NAME;
    }

    @Override
    public String namespace() {
        return ProjectApplicationFormOwnerReferenceFactory.NAMESPACE;
    }

    @Override
    public String slot() {
        return ProjectApplicationFormOwnerReferenceFactory.DEFAULT_SLOT;
    }

    @Override
    public void preflight() {
        List<Long> duplicateProjectIds = jdbcTemplate.queryForList("""
            SELECT project_id
            FROM project_application_form
            GROUP BY project_id
            HAVING COUNT(*) > 1
            ORDER BY project_id
            LIMIT 1
            """, Long.class);
        if (!duplicateProjectIds.isEmpty()) {
            throw new IllegalStateException(
                "project_id가 복수 application Form을 가집니다: " + duplicateProjectIds.getFirst()
            );
        }
    }

    @Override
    public List<FormOwnershipBackfillMapping> loadBatch(long afterParentId, int batchSize) {
        return jdbcTemplate.query("""
            SELECT id AS parent_id, form_id, project_id::text AS owner_resource_key
            FROM project_application_form
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
