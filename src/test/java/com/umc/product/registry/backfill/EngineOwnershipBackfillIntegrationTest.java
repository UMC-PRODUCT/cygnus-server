package com.umc.product.registry.backfill;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.umc.product.feedback.adapter.out.backfill.FeedbackTemplateFormOwnershipBackfillSource;
import com.umc.product.form.adapter.out.backfill.FormOwnershipRolloutAdapter;
import com.umc.product.form.application.port.out.FormOwnershipBackfillSource;
import com.umc.product.notice.adapter.out.backfill.NoticeVoteFormOwnershipBackfillSource;
import com.umc.product.project.adapter.out.backfill.ProjectFormOwnershipBackfillSource;
import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryAdvisoryLockAdapter;
import com.umc.product.registry.adapter.out.jdbc.PostgresRegistryControlAdapter;
import com.umc.product.registry.application.service.RegistryBackfillCoordinator;
import com.umc.product.registry.domain.RegistryDriftType;
import com.umc.product.registry.domain.RegistryReconciliationResult;
import com.umc.product.registry.domain.RegistrySourceReconciliation;
import com.umc.product.registry.domain.RegistryStatus;
import com.umc.product.support.IntegrationTestSupport;

@DisplayName("Form ownership restartable backfill PostgreSQL 통합")
class EngineOwnershipBackfillIntegrationTest extends IntegrationTestSupport {

    private static final Instant CUTOVER_AT = Instant.parse("2026-07-18T03:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void resetFixtures() {
        jdbcTemplate.execute("""
            TRUNCATE TABLE form, project, notice, user_feedback_template,
                registry_backfill_checkpoint, registry_cutover_state
            RESTART IDENTITY CASCADE
            """);
    }

    @Test
    void named_source를_PK_keyset으로_먼저_backfill하고_남은_Form을_standalone으로_채운다() {
        // given
        insertForm(10L);
        insertForm(20L);
        insertForm(30L);
        insertForm(40L);
        insertProjectMapping(1_000L, 100L, 10L);
        insertNoticeMapping(2_000L, 200L, 20L);
        insertFeedbackMapping(300L, 30L);
        FormOwnershipRolloutAdapter rollout = rollout();

        // when
        RegistryReconciliationResult result = coordinator(rollout, 1, 20)
            .backfill(FormOwnershipRolloutAdapter.REGISTRY_NAME);

        // then
        assertThat(result.isClean()).isTrue();
        assertThat(rollout.sourceNames()).containsExactly(
            FeedbackTemplateFormOwnershipBackfillSource.SOURCE_NAME,
            NoticeVoteFormOwnershipBackfillSource.SOURCE_NAME,
            ProjectFormOwnershipBackfillSource.SOURCE_NAME,
            FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME
        );
        assertThat(ownershipRows()).containsExactly(
            "10|project.application-form|100|default",
            "20|notice.vote|200|default",
            "30|feedback.template|300|default",
            "40|form.standalone|40|default"
        );
        assertThat(jdbcTemplate.queryForList("""
            SELECT source_name || ':' || last_parent_id::text
            FROM registry_backfill_checkpoint
            WHERE registry_name = ? AND completed
            ORDER BY source_name
            """, String.class, FormOwnershipRolloutAdapter.REGISTRY_NAME)).containsExactly(
                "feedback-template:300",
                "form-standalone:40",
                "notice-vote:2000",
                "project-application-form:1000"
            );
        assertThat(registryStatus()).isEqualTo(RegistryStatus.VALIDATED.name());
    }

    @Test
    void Project에_application_form이_둘이면_preflight에서_hard_fail한다() {
        // given
        insertForm(10L);
        insertForm(11L);
        insertProjectMapping(1_000L, 100L, 10L);
        insertProjectMapping(1_001L, 100L, 11L);

        // when & then
        assertThatThrownBy(() -> rollout().preflight())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("project_id")
            .hasMessageContaining("100");
        assertThat(ownershipRows()).isEmpty();
    }

    @Test
    void 동일_Form이_서로_다른_named_owner를_가지면_preflight에서_hard_fail한다() {
        // given
        insertForm(10L);
        insertProjectMapping(1_000L, 100L, 10L);
        insertNoticeMapping(2_000L, 200L, 10L);

        // when & then
        assertThatThrownBy(() -> rollout().preflight())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("동일 Form")
            .hasMessageContaining("10");
        assertThat(ownershipRows()).isEmpty();
    }

    @Test
    void 동일_owner_tuple이_서로_다른_Form을_가리키면_preflight에서_hard_fail한다() {
        // given
        insertForm(10L);
        insertForm(11L);
        insertNoticeMapping(2_000L, 200L, 10L);
        insertNoticeMapping(2_001L, 200L, 11L);

        // when & then
        assertThatThrownBy(() -> rollout().preflight())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("owner tuple")
            .hasMessageContaining("notice.vote/200/default");
        assertThat(ownershipRows()).isEmpty();
    }

    @Test
    void broken_stale_ownership_duplicate_drift를_제한적으로_보고하고_기존_binding은_보존한다() {
        // given
        insertForm(10L);
        insertForm(20L);
        insertForm(30L);
        insertProjectMapping(1_000L, 100L, 10L);
        insertNoticeMapping(2_000L, 200L, 20L);
        insertNoticeMapping(2_001L, 200L, 20L);
        insertNoticeMapping(2_010L, 201L, 999L);
        insertOwnership(10L, "form.standalone", "10", "default");
        insertOwnership(30L, "legacy.form", "30", "default");
        insertStaleOwnership(9_999L);
        FormOwnershipRolloutAdapter rollout = rollout();
        RegistryBackfillCoordinator coordinator = coordinator(rollout, 2, 2);

        // when & then
        assertThatThrownBy(() -> coordinator.backfill(FormOwnershipRolloutAdapter.REGISTRY_NAME))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("drift");

        RegistryReconciliationResult first = rollout.reconcile(2);
        RegistryReconciliationResult second = rollout.reconcile(2);
        assertThat(first).isEqualTo(second);
        assertThat(source(first, ProjectFormOwnershipBackfillSource.SOURCE_NAME).counts())
            .containsOnly(
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.SOURCE_ONLY_MISSING, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.OWNERSHIP_CONFLICT, 1L)
            );
        assertThat(source(first, NoticeVoteFormOwnershipBackfillSource.SOURCE_NAME).counts())
            .containsOnly(
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.BROKEN_REFERENCE, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.DUPLICATE_REFERENCE, 1L)
            );
        assertThat(source(first, FormOwnershipRolloutAdapter.STANDALONE_SOURCE_NAME).counts())
            .containsOnly(
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.SOURCE_ONLY_MISSING, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.REGISTRY_ONLY_STALE, 1L),
                org.assertj.core.api.Assertions.entry(
                    RegistryDriftType.OWNERSHIP_CONFLICT, 1L)
            );
        assertThat(first.sources()).allSatisfy(summary ->
            assertThat(summary.details()).hasSizeLessThanOrEqualTo(2));
        assertThat(ownershipRows()).containsExactly(
            "10|form.standalone|10|default",
            "20|notice.vote|200|default",
            "30|legacy.form|30|default",
            "9999|form.standalone|9999|default"
        );
        assertThat(registryStatus()).isEqualTo(RegistryStatus.BLOCKED.name());
    }

    @Test
    void 모든_Form_ownership_rollout_bean은_registry_backfill_profile로_제한된다() {
        // given
        List<Class<?>> rolloutBeans = List.of(
            FormOwnershipRolloutAdapter.class,
            ProjectFormOwnershipBackfillSource.class,
            NoticeVoteFormOwnershipBackfillSource.class,
            FeedbackTemplateFormOwnershipBackfillSource.class
        );

        // when & then
        assertThat(rolloutBeans).allSatisfy(type -> {
            Profile profile = AnnotatedElementUtils.findMergedAnnotation(type, Profile.class);
            assertThat(profile).isNotNull();
            assertThat(profile.value()).containsExactly("registry-backfill");
        });
    }

    private FormOwnershipRolloutAdapter rollout() {
        List<FormOwnershipBackfillSource> sources = List.of(
            new ProjectFormOwnershipBackfillSource(jdbcTemplate),
            new NoticeVoteFormOwnershipBackfillSource(jdbcTemplate),
            new FeedbackTemplateFormOwnershipBackfillSource(jdbcTemplate)
        );
        return new FormOwnershipRolloutAdapter(jdbcTemplate, sources);
    }

    private RegistryBackfillCoordinator coordinator(
        FormOwnershipRolloutAdapter rollout,
        int batchSize,
        int detailLimit
    ) {
        return new RegistryBackfillCoordinator(
            List.of(rollout),
            new PostgresRegistryControlAdapter(jdbcTemplate),
            new PostgresRegistryAdvisoryLockAdapter(dataSource),
            Clock.fixed(CUTOVER_AT, ZoneOffset.UTC),
            batchSize,
            detailLimit
        );
    }

    private RegistrySourceReconciliation source(
        RegistryReconciliationResult result,
        String sourceName
    ) {
        return result.sources().stream()
            .filter(source -> source.sourceName().equals(sourceName))
            .findFirst()
            .orElseThrow();
    }

    private String registryStatus() {
        return jdbcTemplate.queryForObject("""
            SELECT status
            FROM registry_cutover_state
            WHERE registry_name = ?
            """, String.class, FormOwnershipRolloutAdapter.REGISTRY_NAME);
    }

    private List<String> ownershipRows() {
        return jdbcTemplate.queryForList("""
            SELECT form_id::text || '|' || namespace || '|' || owner_resource_key || '|' || slot
            FROM form_ownership
            ORDER BY form_id
            """, String.class);
    }

    private void insertForm(long formId) {
        jdbcTemplate.update("""
            INSERT INTO form
                (id, created_at, updated_at, created_member_id, title, status,
                 is_anonymous, allow_duplicate_responses)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, ?, 'DRAFT', FALSE, FALSE)
            """, formId, "Form " + formId);
    }

    private void insertProjectMapping(long mappingId, long projectId, long formId) {
        jdbcTemplate.update("""
            INSERT INTO project
                (id, created_at, updated_at, gisu_id, chapter_id, status,
                 product_owner_member_id, product_owner_school_id, created_by_member_id)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?, 'DRAFT', ?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, projectId, projectId, projectId, projectId, projectId, projectId);
        jdbcTemplate.update("""
            INSERT INTO project_application_form
                (id, created_at, updated_at, project_id, form_id)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?)
            """, mappingId, projectId, formId);
    }

    private void insertNoticeMapping(long mappingId, long noticeId, long formId) {
        jdbcTemplate.update("""
            INSERT INTO notice
                (id, should_send_notification, author_member_id, created_at, updated_at,
                 content, title)
            VALUES (?, FALSE, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '내용', '제목')
            ON CONFLICT (id) DO NOTHING
            """, noticeId);
        jdbcTemplate.update("""
            INSERT INTO notice_vote
                (id, created_at, updated_at, notice_id, vote_id, starts_at, ends_at_exclusive)
            VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?, ?,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 day')
            """, mappingId, noticeId, formId);
    }

    private void insertFeedbackMapping(long templateId, long formId) {
        jdbcTemplate.update("""
            INSERT INTO user_feedback_template
                (id, context, target_type, form_id, is_active, created_at, updated_at)
            VALUES (?, 'APPLICATION_SUBMITTED', 'NEW_CHALLENGER', ?, TRUE,
                    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, templateId, formId);
    }

    private void insertOwnership(
        long formId,
        String namespace,
        String ownerResourceKey,
        String slot
    ) {
        jdbcTemplate.update("""
            INSERT INTO form_ownership
                (form_id, namespace, owner_resource_key, slot, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, formId, namespace, ownerResourceKey, slot);
    }

    private void insertStaleOwnership(long formId) {
        jdbcTemplate.execute("ALTER TABLE form_ownership DISABLE TRIGGER ALL");
        try {
            insertOwnership(formId, "form.standalone", Long.toString(formId), "default");
        } finally {
            jdbcTemplate.execute("ALTER TABLE form_ownership ENABLE TRIGGER ALL");
        }
    }
}
