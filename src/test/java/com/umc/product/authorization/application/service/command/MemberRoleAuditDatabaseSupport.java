package com.umc.product.authorization.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.support.IntegrationTestSupport;

abstract class MemberRoleAuditDatabaseSupport extends IntegrationTestSupport {

    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventOutboxRelayService eventOutboxRelayService;

    protected AuditRow explicitRow(String targetType, Long targetId, String action) {
        List<AuditRow> rows = jdbcTemplate.query("""
            SELECT action, target_type, target_id, actor_member_id, description, details::text AS details
              FROM audit_log
             WHERE target_type = ?
               AND target_id = ?
               AND action = ?
               AND source = 'EXPLICIT_RECORDER'
            """, (resultSet, rowNumber) -> new AuditRow(
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getObject("actor_member_id", Long.class),
                resultSet.getString("description"),
                resultSet.getString("details")
            ), targetType, targetId.toString(), action);
        assertThat(rows).singleElement();
        return rows.getFirst();
    }

    protected List<AuditRow> explicitRows(String targetType) {
        return jdbcTemplate.query("""
            SELECT action, target_type, target_id, actor_member_id, description, details::text AS details
              FROM audit_log
             WHERE target_type = ?
               AND source = 'EXPLICIT_RECORDER'
             ORDER BY id
            """, (resultSet, rowNumber) -> new AuditRow(
                resultSet.getString("action"),
                resultSet.getString("target_type"),
                resultSet.getString("target_id"),
                resultSet.getObject("actor_member_id", Long.class),
                resultSet.getString("description"),
                resultSet.getString("details")
            ), targetType);
    }

    protected JsonNode details(AuditRow row) throws JsonProcessingException {
        return objectMapper.readTree(row.detailsJson());
    }

    protected void awaitExplicitRowCount(String targetType, int expected) {
        relayAuditEvents();
        await().atMost(ASYNC_TIMEOUT).untilAsserted(() ->
            assertThat(explicitRows(targetType)).hasSize(expected)
        );
    }

    protected void relayAuditEvents() {
        eventOutboxRelayService.relay();
    }

    protected long memberCount(Long memberId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM member WHERE id = ?",
            Long.class,
            memberId
        );
    }

    protected long roleCount(Long roleId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM challenger_role WHERE id = ?",
            Long.class,
            roleId
        );
    }

    protected long totalRoleCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM challenger_role", Long.class);
    }

    protected long successAuditCount(String targetType) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE target_type = ? AND outcome = 'SUCCESS'",
            Long.class,
            targetType
        );
    }

    protected long successAuditCount(String targetType, String action) {
        return jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
              FROM audit_log
             WHERE target_type = ?
               AND action = ?
               AND outcome = 'SUCCESS'
            """, Long.class, targetType, action);
    }

    protected String roleType(Long roleId) {
        return jdbcTemplate.queryForObject(
            "SELECT role_type FROM challenger_role WHERE id = ?",
            String.class,
            roleId
        );
    }

    protected record AuditRow(
        String action,
        String targetType,
        String targetId,
        Long actorMemberId,
        String description,
        String detailsJson
    ) {
    }
}
