package com.umc.product.form.adapter.out.backfill;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;

final class FormOwnershipRegistrySnapshot {

    private final List<FormOwnershipRegistryRow> rows;
    private final Set<FormOwnershipBinding> bindings;
    private final Set<Long> formIds;
    private final Set<FormOwnershipCoordinate> coordinates;

    private FormOwnershipRegistrySnapshot(List<FormOwnershipRegistryRow> rows) {
        this.rows = rows;
        this.bindings = new LinkedHashSet<>();
        this.formIds = new HashSet<>();
        this.coordinates = new HashSet<>();
        rows.forEach(row -> {
            bindings.add(FormOwnershipBinding.from(row));
            formIds.add(row.formId());
            coordinates.add(FormOwnershipCoordinate.from(row));
        });
    }

    static FormOwnershipRegistrySnapshot load(JdbcTemplate jdbcTemplate) {
        List<FormOwnershipRegistryRow> rows = jdbcTemplate.query("""
            SELECT form_id, namespace, owner_resource_key, slot
            FROM form_ownership
            ORDER BY form_id
            """, (resultSet, rowNum) -> new FormOwnershipRegistryRow(
                resultSet.getLong("form_id"),
                resultSet.getString("namespace"),
                resultSet.getString("owner_resource_key"),
                resultSet.getString("slot")
            ));
        return new FormOwnershipRegistrySnapshot(rows);
    }

    List<FormOwnershipRegistryRow> rows() {
        return rows;
    }

    boolean contains(FormOwnershipCandidate candidate) {
        return bindings.contains(FormOwnershipBinding.from(candidate));
    }

    boolean containsForm(long formId) {
        return formIds.contains(formId);
    }

    boolean containsCoordinate(FormOwnershipCandidate candidate) {
        return coordinates.contains(FormOwnershipCoordinate.from(candidate));
    }
}
