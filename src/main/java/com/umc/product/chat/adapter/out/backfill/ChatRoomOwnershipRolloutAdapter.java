package com.umc.product.chat.adapter.out.backfill;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.umc.product.chat.domain.ChatRoomOwnerReference;
import com.umc.product.registry.application.port.out.RegistryRolloutPort;
import com.umc.product.registry.domain.RegistryBackfillBatch;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;

@Profile("registry-backfill")
@Component
public class ChatRoomOwnershipRolloutAdapter implements RegistryRolloutPort {

    public static final String REGISTRY_NAME = "chat-room-ownership";
    public static final String SOURCE_NAME = "chat-room";

    private static final String BACKFILL_BATCH_SQL = """
        WITH source_batch AS (
            SELECT room.id
            FROM chat_room room
            WHERE room.id > ?
            ORDER BY room.id
            LIMIT ?
        ), inserted AS (
            INSERT INTO chat_room_ownership
                (room_id, namespace, owner_resource_key, slot, created_at, updated_at)
            SELECT batch.id, ?, batch.id::text, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
            FROM source_batch batch
            ON CONFLICT DO NOTHING
            RETURNING room_id
        )
        SELECT COALESCE(MAX(batch.id), ?) AS last_parent_id,
               COUNT(*) AS scanned_parents
        FROM source_batch batch
        """;

    private static final String RECONCILE_SQL = """
        WITH drifts AS (
            SELECT 1 AS drift_order,
                   'SOURCE_ONLY_MISSING' AS drift_type,
                   room.id AS room_id,
                   'SOURCE_ONLY_MISSING:room_id=' || room.id::text AS detail
            FROM chat_room room
            LEFT JOIN chat_room_ownership ownership ON ownership.room_id = room.id
            WHERE ownership.room_id IS NULL
            UNION ALL
            SELECT 2 AS drift_order,
                   'REGISTRY_ONLY_STALE' AS drift_type,
                   ownership.room_id,
                   'REGISTRY_ONLY_STALE:room_id=' || ownership.room_id::text AS detail
            FROM chat_room_ownership ownership
            LEFT JOIN chat_room room ON room.id = ownership.room_id
            WHERE room.id IS NULL
            UNION ALL
            SELECT 3 AS drift_order,
                   'OWNERSHIP_CONFLICT' AS drift_type,
                   room.id AS room_id,
                   'OWNERSHIP_CONFLICT:room_id=' || room.id::text
                       || ',actual=' || ownership.namespace || '/'
                       || ownership.owner_resource_key || '/' || ownership.slot
                       || ',expected=' || ? || '/' || room.id::text || '/' || ? AS detail
            FROM chat_room room
            JOIN chat_room_ownership ownership ON ownership.room_id = room.id
            WHERE ownership.namespace <> ?
               OR ownership.owner_resource_key <> room.id::text
               OR ownership.slot <> ?
        ), counts AS (
            SELECT drift_order, drift_type, COUNT(*) AS drift_count
            FROM drifts
            GROUP BY drift_order, drift_type
        ), details AS (
            SELECT drift_order, drift_type, room_id, detail
            FROM drifts
            ORDER BY drift_order, room_id
            LIMIT ?
        )
        SELECT counts.drift_order,
               counts.drift_type,
               counts.drift_count,
               details.room_id,
               details.detail
        FROM counts
        LEFT JOIN details
          ON details.drift_order = counts.drift_order
         AND details.drift_type = counts.drift_type
        ORDER BY counts.drift_order, details.room_id
        """;

    private final JdbcTemplate jdbcTemplate;

    public ChatRoomOwnershipRolloutAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String registryName() {
        return REGISTRY_NAME;
    }

    @Override
    public List<String> sourceNames() {
        return List.of(SOURCE_NAME);
    }

    @Override
    public void preflight() {
        Boolean tablesPresent = jdbcTemplate.queryForObject("""
            SELECT to_regclass('chat_room') IS NOT NULL
               AND to_regclass('chat_room_ownership') IS NOT NULL
            """, Boolean.class);
        if (!Boolean.TRUE.equals(tablesPresent)) {
            throw new IllegalStateException("chat room ownership backfill 대상 테이블이 없습니다.");
        }
    }

    @Override
    public RegistryBackfillBatch backfillBatch(
        String sourceName,
        long afterParentId,
        int batchSize
    ) {
        requireSource(sourceName);
        if (afterParentId < 0L || batchSize <= 0) {
            throw new IllegalArgumentException("parent id는 음수가 아니고 batch size는 양수여야 합니다.");
        }
        return jdbcTemplate.queryForObject(
            BACKFILL_BATCH_SQL,
            (resultSet, rowNumber) -> {
                long scannedParents = resultSet.getLong("scanned_parents");
                return new RegistryBackfillBatch(
                    resultSet.getLong("last_parent_id"),
                    scannedParents,
                    scannedParents < batchSize
                );
            },
            afterParentId,
            batchSize,
            ChatRoomOwnerReference.STANDALONE_NAMESPACE,
            ChatRoomOwnerReference.DEFAULT_SLOT,
            afterParentId
        );
    }

    @Override
    public RegistryReconciliationResult reconcile(int detailLimit) {
        if (detailLimit <= 0) {
            throw new IllegalArgumentException("detail limit은 양수여야 합니다.");
        }
        Map<RegistryDriftType, Long> counts = new EnumMap<>(RegistryDriftType.class);
        List<String> details = new ArrayList<>();
        jdbcTemplate.query(
            RECONCILE_SQL,
            resultSet -> {
                while (resultSet.next()) {
                    RegistryDriftType type = RegistryDriftType.valueOf(
                        resultSet.getString("drift_type"));
                    counts.put(type, resultSet.getLong("drift_count"));
                    String detail = resultSet.getString("detail");
                    if (detail != null) {
                        details.add(detail);
                    }
                }
                return null;
            },
            ChatRoomOwnerReference.STANDALONE_NAMESPACE,
            ChatRoomOwnerReference.DEFAULT_SLOT,
            ChatRoomOwnerReference.STANDALONE_NAMESPACE,
            ChatRoomOwnerReference.DEFAULT_SLOT,
            detailLimit
        );
        RegistrySourceReconciliation source = new RegistrySourceReconciliation(
            SOURCE_NAME, counts, details);
        return new RegistryReconciliationResult(REGISTRY_NAME, List.of(source));
    }

    private static void requireSource(String sourceName) {
        if (!SOURCE_NAME.equals(sourceName)) {
            throw new IllegalArgumentException("지원하지 않는 chat ownership source입니다: " + sourceName);
        }
    }
}
