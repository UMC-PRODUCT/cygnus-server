package com.umc.product.chat.adapter.out.persistence;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.chat.application.port.out.LoadChatOwnershipNamespacesPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatOwnershipNamespacePersistenceAdapter
    implements LoadChatOwnershipNamespacesPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<String> loadDistinctNamespaces() {
        return jdbcTemplate.queryForList(
            "SELECT DISTINCT namespace FROM chat_room_ownership ORDER BY namespace",
            String.class
        );
    }
}
