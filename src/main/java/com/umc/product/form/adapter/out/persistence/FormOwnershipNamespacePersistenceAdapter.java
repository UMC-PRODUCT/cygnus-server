package com.umc.product.form.adapter.out.persistence;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.form.application.port.out.LoadFormOwnershipNamespacesPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FormOwnershipNamespacePersistenceAdapter
    implements LoadFormOwnershipNamespacesPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> loadDistinctNamespaces() {
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT namespace FROM form_ownership ORDER BY namespace",
            String.class
        );
    }
}
