package com.umc.product.challenger.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.challenger.application.port.in.command.ManageChallengerRecordUseCase;
import com.umc.product.challenger.application.port.in.command.dto.ConsumeChallengerRecordCommand;
import com.umc.product.challenger.application.port.in.command.dto.CreateChallengerRecordCommand;
import com.umc.product.challenger.application.port.out.SaveChallengerRecordPort;
import com.umc.product.challenger.domain.ChallengerRecord;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("챌린저 기록 rich audit 통합 테스트")
class ChallengerRecordRichAuditIntegrationTest extends IntegrationTestSupport {

    private static final String RECORD_CODE_SENTINEL = "RECORD-CODE-MUST-NOT-PERSIST";
    private static final Duration AUDIT_TIMEOUT = Duration.ofSeconds(10);

    @Autowired
    private ManageChallengerRecordUseCase manageChallengerRecordUseCase;

    @Autowired
    private SaveChallengerRecordPort saveChallengerRecordPort;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private LoadGisuPort loadGisuPort;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EventOutboxRelayService eventOutboxRelayService;

    @Test
    @DisplayName("단건 생성 감사 로그는 생성 당시 회원과 기수 및 기록 유형 snapshot을 보존한다")
    void 단건_생성_감사_로그는_당시_snapshot을_보존한다() throws Exception {
        // given
        RecordContext context = recordContext(9301L, "단건감사");
        Member creator = member("단건관리자", "단건닉네임", context.school().getId());
        CreateChallengerRecordCommand command = CreateChallengerRecordCommand.builder()
            .creatorMemberId(creator.getId())
            .gisuId(context.gisu().getId())
            .chapterId(context.chapter().getId())
            .schoolId(context.school().getId())
            .part(ChallengerPart.SPRINGBOOT)
            .memberName("단건대상")
            .build();

        // when
        Long recordId = manageChallengerRecordUseCase.create(command);
        creator.updateProfile("변경된닉네임", null);
        saveMemberPort.save(creator);

        // then
        Map<String, Object> row = richAuditRow(recordId);
        JsonNode details = objectMapper.readTree(row.get("details").toString());
        assertThat(row.get("actor_member_id")).isEqualTo(command.creatorMemberId());
        assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(command.creatorMemberId());
        assertThat(details.path("actor").path("name").asText()).isEqualTo("단건관리자");
        assertThat(details.path("actor").path("nickname").asText()).isEqualTo("단건닉네임");
        assertThat(details.path("target").path("id").asText()).isEqualTo(recordId.toString());
        assertThat(details.path("target").path("name").asText()).isEqualTo("단건대상");
        assertThat(details.path("target").path("term").asLong()).isEqualTo(context.gisu().getId());
        assertThat(details.path("target").path("recordType").asText()).isEqualTo("CHALLENGER");
    }

    @Test
    @DisplayName("일괄 생성은 각 기록별 회원과 기수 및 기록 유형 snapshot을 저장한다")
    void 일괄_생성은_각_기록별_snapshot을_저장한다() throws Exception {
        // given
        RecordContext context = recordContext(9302L, "일괄감사");
        Member creator = member("일괄관리자", "일괄닉네임", context.school().getId());
        List<CreateChallengerRecordCommand> commands = List.of(
            recordCommand(creator.getId(), context, "일괄일반", null),
            recordCommand(creator.getId(), context, "일괄운영진", ChallengerRoleType.SCHOOL_PRESIDENT)
        );

        // when
        List<Long> recordIds = manageChallengerRecordUseCase.createBulk(commands);

        // then
        List<Map<String, Object>> rows = richAuditRows();
        assertThat(rows).hasSize(2);
        assertThat(rows)
            .extracting(row -> row.get("target_id").toString())
            .containsExactlyInAnyOrderElementsOf(recordIds.stream().map(String::valueOf).toList());

        Set<String> targetNames = new HashSet<>();
        Set<String> recordTypes = new HashSet<>();
        for (Map<String, Object> row : rows) {
            JsonNode details = objectMapper.readTree(row.get("details").toString());
            assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(creator.getId());
            assertThat(details.path("target").path("term").asLong()).isEqualTo(context.gisu().getId());
            targetNames.add(details.path("target").path("name").asText());
            recordTypes.add(details.path("target").path("recordType").asText());
        }
        assertThat(targetNames).containsExactlyInAnyOrder("일괄일반", "일괄운영진");
        assertThat(recordTypes).containsExactlyInAnyOrder("CHALLENGER", "CHALLENGER_ROLE");
    }

    @Test
    @DisplayName("코드 사용 감사 로그는 사용 회원과 기수 및 기록 유형 snapshot을 보존하고 코드는 제외한다")
    void 코드_사용_감사_로그는_회원_snapshot을_보존하고_코드를_제외한다() throws Exception {
        // given
        RecordContext context = recordContext(9303L, "코드감사");
        Member targetMember = member("코드사용자", "사용전닉네임", context.school().getId());
        Member creator = member("코드발급자", "발급자닉네임", context.school().getId());
        ChallengerRecord record = saveChallengerRecordPort.save(ChallengerRecord.create(
            creator.getId(),
            context.gisu().getId(),
            context.chapter().getId(),
            context.school().getId(),
            ChallengerPart.ANDROID,
            targetMember.getName()
        ));
        ConsumeChallengerRecordCommand command = ConsumeChallengerRecordCommand.builder()
            .targetMemberId(targetMember.getId())
            .code(record.getCode())
            .build();

        // when
        manageChallengerRecordUseCase.consumeCode(command);
        targetMember.updateProfile("사용후닉네임", null);
        saveMemberPort.save(targetMember);

        // then
        Map<String, Object> row = richAuditRow(record.getId());
        String detailsJson = row.get("details").toString();
        JsonNode details = objectMapper.readTree(detailsJson);
        assertThat(row.get("actor_member_id")).isEqualTo(targetMember.getId());
        assertThat(details.path("actor").path("nickname").asText()).isEqualTo("사용전닉네임");
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(targetMember.getId());
        assertThat(details.path("target").path("term").asLong()).isEqualTo(context.gisu().getId());
        assertThat(details.path("target").path("recordType").asText()).isEqualTo("CHALLENGER");
        assertThat(detailsJson).doesNotContain("code", record.getCode(), RECORD_CODE_SENTINEL);
    }

    private CreateChallengerRecordCommand recordCommand(
        Long creatorMemberId,
        RecordContext context,
        String memberName,
        ChallengerRoleType roleType
    ) {
        return CreateChallengerRecordCommand.builder()
            .creatorMemberId(creatorMemberId)
            .gisuId(context.gisu().getId())
            .chapterId(context.chapter().getId())
            .schoolId(context.school().getId())
            .part(ChallengerPart.WEB)
            .memberName(memberName)
            .challengerRoleType(roleType)
            .build();
    }

    private Map<String, Object> richAuditRow(Long targetId) {
        eventOutboxRelayService.relay();
        await().atMost(AUDIT_TIMEOUT).untilAsserted(() ->
            assertThat(queryRichAuditRows(targetId)).singleElement()
        );
        return queryRichAuditRows(targetId).getFirst();
    }

    private List<Map<String, Object>> richAuditRows() {
        eventOutboxRelayService.relay();
        await().atMost(AUDIT_TIMEOUT).untilAsserted(() ->
            assertThat(queryRichAuditRows(null)).hasSize(2)
        );
        return queryRichAuditRows(null);
    }

    private List<Map<String, Object>> queryRichAuditRows(Long targetId) {
        if (targetId != null) {
            return jdbcTemplate.queryForList("""
                SELECT target_id, actor_member_id, details::text AS details
                  FROM audit_log
                 WHERE target_type = 'ChallengerRecord'
                   AND target_id = ?
                   AND source = 'EXPLICIT_RECORDER'
                 ORDER BY id
                """, targetId.toString());
        }
        return jdbcTemplate.queryForList("""
            SELECT target_id, actor_member_id, details::text AS details
              FROM audit_log
             WHERE target_type = 'ChallengerRecord'
               AND source = 'EXPLICIT_RECORDER'
             ORDER BY id
            """);
    }

    private RecordContext recordContext(Long generation, String prefix) {
        Gisu gisu = loadGisuPort.findActiveGisu()
            .orElseGet(() -> gisuFixture.활성_기수(generation));
        Chapter chapter = chapterFixture.지부(gisu, prefix + "지부");
        School school = schoolFixture.지부에_소속된_학교(prefix + "학교", chapter);
        return new RecordContext(gisu, chapter, school);
    }

    private Member member(String name, String nickname, Long schoolId) {
        return saveMemberPort.save(Member.create(
            name,
            nickname,
            name + "-" + nickname + "@audit.test",
            schoolId,
            null
        ));
    }

    private record RecordContext(Gisu gisu, Chapter chapter, School school) {
    }
}
