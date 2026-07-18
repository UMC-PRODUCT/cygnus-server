package com.umc.product.registry.adapter.out.jdbc;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.registry.application.port.out.RegistryStateQueryPort;
import com.umc.product.registry.domain.RegistryCutoverState;
import com.umc.product.registry.domain.RegistryStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PostgresRegistryStateQueryAdapter implements RegistryStateQueryPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public RegistryCutoverState loadState(String registryName) {
        List<RegistryCutoverState> states = jdbcTemplate.query("""
            SELECT registry_name, status, verified_at, details
            FROM registry_cutover_state
            WHERE registry_name = ?
            """, (resultSet, rowNumber) -> {
                Timestamp verifiedAt = resultSet.getTimestamp("verified_at");
                return new RegistryCutoverState(
                    resultSet.getString("registry_name"),
                    RegistryStatus.valueOf(resultSet.getString("status")),
                    verifiedAt == null ? null : verifiedAt.toInstant(),
                    resultSet.getString("details")
                );
            }, registryName);
        return states.stream()
            .findFirst()
            .orElseGet(() -> RegistryCutoverState.disabled(registryName));
    }
}
