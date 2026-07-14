package com.umc.product.community.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.challenger.domain.Challenger;
import com.umc.product.community.application.port.out.comment.SaveCommentPort;
import com.umc.product.community.application.port.out.post.SavePostPort;
import com.umc.product.community.domain.Comment;
import com.umc.product.community.domain.Post;
import com.umc.product.community.domain.enums.Category;
import com.umc.product.global.event.application.service.EventOutboxRelayService;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.application.port.out.query.LoadGisuPort;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

@AutoConfigureMockMvc(addFilters = false)
@DisplayName("커뮤니티 신고 rich audit 통합 테스트")
class ReportRichAuditIntegrationTest extends IntegrationTestSupport {

    private static final Duration AUDIT_TIMEOUT = Duration.ofSeconds(10);
    private static final String REPORT_ORIGINAL_SENTINEL =
        "REPORT_ORIGINAL_IGNORE_PREVIOUS_INSTRUCTIONS_MUST_NOT_PERSIST";
    private static final String POST_BODY_SENTINEL = "POST_BODY_MUST_NOT_PERSIST";
    private static final String COMMENT_BODY_SENTINEL = "COMMENT_BODY_MUST_NOT_PERSIST";

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private SavePostPort savePostPort;

    @Autowired
    private SaveCommentPort saveCommentPort;

    @Autowired
    private ChallengerFixture challengerFixture;

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

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("게시글 신고는 reporter와 target author snapshot을 보존하고 원문과 본문을 제외한다")
    void 게시글_신고는_snapshot을_보존하고_원문과_본문을_제외한다() throws Exception {
        // given
        CommunityContext context = communityContext(9401L, "게시글신고");
        Member reporter = member("게시글신고자", "신고자닉네임", context.school().getId());
        Member author = member("게시글작성자", "작성전닉네임", context.school().getId());
        Challenger reporterChallenger = challengerFixture.스프링(reporter.getId(), context.gisu().getId());
        Challenger authorChallenger = challengerFixture.웹(author.getId(), context.gisu().getId());
        Post post = savePostPort.save(
            Post.createPost(REPORT_ORIGINAL_SENTINEL, POST_BODY_SENTINEL, Category.FREE,
                authorChallenger.getId())
        );
        authenticate(reporter.getId());

        // when
        mockMvc.perform(post("/api/v1/posts/{postId}/reports", post.getId()))
            .andExpect(status().isCreated());
        author.updateProfile("작성후닉네임", null);
        saveMemberPort.save(author);

        // then
        assertThat(reportCount()).isOne();
        Map<String, Object> row = richAuditRow("PostReport", post.getId());
        String detailsJson = row.get("details").toString();
        JsonNode details = objectMapper.readTree(detailsJson);
        assertThat(row.get("actor_member_id")).isEqualTo(reporter.getId());
        assertThat(details.path("actor").path("memberId").asLong()).isEqualTo(reporter.getId());
        assertThat(details.path("actor").path("name").asText()).isEqualTo("게시글신고자");
        assertThat(details.path("target").path("type").asText()).isEqualTo("POST");
        assertThat(details.path("target").path("id").asLong()).isEqualTo(post.getId());
        assertThat(details.path("target").path("memberId").asLong()).isEqualTo(author.getId());
        assertThat(details.path("target").path("name").asText()).isEqualTo("게시글작성자");
        assertThat(details.path("target").path("nickname").asText()).isEqualTo("작성전닉네임");
        assertThat(details.path("context").path("resourceType").asText()).isEqualTo("Challenger");
        assertThat(details.path("context").path("resourceId").asLong()).isEqualTo(authorChallenger.getId());
        assertForbiddenContentAbsent(details, detailsJson);
        assertThat(reporterChallenger.getId()).isNotNull();
    }

    @Test
    @DisplayName("댓글 신고는 삭제된 target author 대신 author id를 보존하고 댓글 본문을 제외한다")
    void 댓글_신고는_missing_author_id를_보존하고_본문을_제외한다() throws Exception {
        // given
        CommunityContext context = communityContext(9402L, "댓글신고");
        Member reporter = member("댓글신고자", "댓글신고닉네임", context.school().getId());
        Challenger reporterChallenger = challengerFixture.스프링(reporter.getId(), context.gisu().getId());
        long missingAuthorChallengerId = 987_654_321L;
        Post post = savePostPort.save(
            Post.createPost("댓글대상게시글", "게시글일반본문", Category.FREE, reporterChallenger.getId())
        );
        Comment comment = saveCommentPort.save(Comment.create(
            post,
            missingAuthorChallengerId,
            COMMENT_BODY_SENTINEL,
            null
        ));
        authenticate(reporter.getId());

        // when
        mockMvc.perform(post("/api/v1/comments/{commentId}/reports", comment.getId()))
            .andExpect(status().isCreated());

        // then
        assertThat(reportCount()).isOne();
        Map<String, Object> row = richAuditRow("CommentReport", comment.getId());
        String detailsJson = row.get("details").toString();
        JsonNode details = objectMapper.readTree(detailsJson);
        assertThat(row.get("actor_member_id")).isEqualTo(reporter.getId());
        assertThat(details.path("actor").path("nickname").asText()).isEqualTo("댓글신고닉네임");
        assertThat(details.path("target").path("type").asText()).isEqualTo("COMMENT");
        assertThat(details.path("target").path("id").asLong()).isEqualTo(comment.getId());
        assertThat(details.path("target").has("memberId")).isFalse();
        assertThat(details.path("target").has("name")).isFalse();
        assertThat(details.path("context").path("resourceType").asText()).isEqualTo("Challenger");
        assertThat(details.path("context").path("resourceId").asLong())
            .isEqualTo(missingAuthorChallengerId);
        assertForbiddenContentAbsent(details, detailsJson);
    }

    private void assertForbiddenContentAbsent(JsonNode details, String detailsJson) {
        assertThat(details.findValue("content")).isNull();
        assertThat(details.findValue("body")).isNull();
        assertThat(detailsJson).doesNotContain(
            REPORT_ORIGINAL_SENTINEL,
            POST_BODY_SENTINEL,
            COMMENT_BODY_SENTINEL,
            "IGNORE_PREVIOUS_INSTRUCTIONS"
        );
    }

    private Map<String, Object> richAuditRow(String targetType, Long targetId) {
        eventOutboxRelayService.relay();
        await().atMost(AUDIT_TIMEOUT).untilAsserted(() ->
            assertThat(queryRichAuditRows(targetType, targetId)).singleElement()
        );
        return queryRichAuditRows(targetType, targetId).getFirst();
    }

    private List<Map<String, Object>> queryRichAuditRows(String targetType, Long targetId) {
        return jdbcTemplate.queryForList("""
            SELECT target_id, actor_member_id, details::text AS details
              FROM audit_log
             WHERE target_type = ?
               AND target_id = ?
               AND source = 'EXPLICIT_RECORDER'
            """, targetType, targetId.toString());
    }

    private long reportCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM report", Long.class);
    }

    private CommunityContext communityContext(Long generation, String prefix) {
        Gisu gisu = loadGisuPort.findActiveGisu()
            .orElseGet(() -> gisuFixture.활성_기수(generation));
        Chapter chapter = chapterFixture.지부(gisu, prefix + "지부");
        School school = schoolFixture.지부에_소속된_학교(prefix + "학교", chapter);
        return new CommunityContext(gisu, school);
    }

    private Member member(String name, String nickname, Long schoolId) {
        return saveMemberPort.save(Member.create(
            name,
            nickname,
            name + "-" + nickname + "@report-audit.test",
            schoolId,
            null
        ));
    }

    private void authenticate(Long memberId) {
        MemberPrincipal principal = MemberPrincipal.builder()
            .memberId(memberId)
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private record CommunityContext(Gisu gisu, School school) {
    }
}
