package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.umc.product.chat.adapter.out.backfill.ChatRoomOwnershipRolloutAdapter;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.support.IntegrationTestSupport;

@ActiveProfiles({"test", "registry-backfill"})
@DisplayName("Chat room ownership standalone backfill PostgreSQL 통합")
class ChatRoomOwnershipBackfillIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ChatRoomOwnershipRolloutAdapter rollout;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void chat_room을_PK_keyset으로_exact_backfill하고_재실행해도_같은_binding을_유지한다() {
        // given
        insertRoom(10L);
        insertRoom(20L);
        insertRoom(30L);
        insertOwnership(20L, "chat.standalone", "20", "default");

        // when
        rollout.preflight();
        RegistryBackfillBatch first = rollout.backfillBatch(
            ChatRoomOwnershipRolloutAdapter.SOURCE_NAME, 0L, 2);
        RegistryBackfillBatch second = rollout.backfillBatch(
            ChatRoomOwnershipRolloutAdapter.SOURCE_NAME, first.lastParentId(), 2);
        rollout.backfillBatch(ChatRoomOwnershipRolloutAdapter.SOURCE_NAME, 0L, 2);
        rollout.backfillBatch(
            ChatRoomOwnershipRolloutAdapter.SOURCE_NAME, first.lastParentId(), 2);

        // then
        assertThat(rollout.registryName())
            .isEqualTo(ChatRoomOwnershipRolloutAdapter.REGISTRY_NAME);
        assertThat(rollout.sourceNames())
            .containsExactly(ChatRoomOwnershipRolloutAdapter.SOURCE_NAME);
        assertThat(first).isEqualTo(new RegistryBackfillBatch(20L, 2L, false));
        assertThat(second).isEqualTo(new RegistryBackfillBatch(30L, 1L, true));
        assertThat(ownershipRows()).containsExactly(
            "10|chat.standalone|10|default",
            "20|chat.standalone|20|default",
            "30|chat.standalone|30|default"
        );
        assertThat(rollout.reconcile(10).isClean()).isTrue();
    }

    @Test
    void 기존_binding과_stale_row를_수정하지_않고_drift를_정확하고_제한적으로_보고한다() {
        // given
        insertRoom(100L);
        insertRoom(200L);
        insertOwnership(100L, "chat.standalone", "200", "default");
        insertStaleOwnership(999L);
        List<String> rowsBefore = ownershipRows();

        // when
        rollout.backfillBatch(ChatRoomOwnershipRolloutAdapter.SOURCE_NAME, 0L, 10);
        RegistryReconciliationResult first = rollout.reconcile(2);
        RegistryReconciliationResult second = rollout.reconcile(2);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.sources()).singleElement().satisfies(source -> {
            assertThat(source.sourceName())
                .isEqualTo(ChatRoomOwnershipRolloutAdapter.SOURCE_NAME);
            assertThat(source.counts()).containsOnly(
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.SOURCE_ONLY_MISSING, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.REGISTRY_ONLY_STALE, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.OWNERSHIP_CONFLICT, 1L)
            );
            assertThat(source.details()).containsExactly(
                "REGISTRY_ONLY_STALE:room_id=999",
                "SOURCE_ONLY_MISSING:room_id=200"
            );
        });
        assertThat(ownershipRows()).containsExactlyElementsOf(rowsBefore);
        assertThat(ownershipRows()).containsExactly(
            "100|chat.standalone|200|default",
            "999|chat.standalone|999|default"
        );
    }

    private void insertRoom(long roomId) {
        jdbcTemplate.update("""
            INSERT INTO chat_room (id, created_at, updated_at)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, roomId);
    }

    private void insertOwnership(
        long roomId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        jdbcTemplate.update("""
            INSERT INTO chat_room_ownership
                (room_id, namespace, owner_resource_key, slot, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, roomId, namespace, ownerResourceKey, slot);
    }

    private void insertStaleOwnership(long roomId) {
        jdbcTemplate.execute("ALTER TABLE chat_room_ownership DISABLE TRIGGER ALL");
        try {
            insertOwnership(roomId, "chat.standalone", Long.toString(roomId), "default");
        } finally {
            jdbcTemplate.execute("ALTER TABLE chat_room_ownership ENABLE TRIGGER ALL");
        }
    }

    private List<String> ownershipRows() {
        return jdbcTemplate.queryForList("""
            SELECT room_id::text || '|' || namespace || '|' || owner_resource_key || '|' || slot
            FROM chat_room_ownership
            ORDER BY room_id
            """, String.class);
    }
}
