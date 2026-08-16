package com.umc.product.blog.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import com.umc.product.blog.application.port.out.SaveBlogContentPort;
import com.umc.product.blog.domain.BlogContent;
import com.umc.product.blog.domain.BlogContentStatus;
import com.umc.product.blog.domain.BlogContentType;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.member.adapter.out.persistence.MemberSystemRoleJpaRepository;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.member.domain.MemberSystemRole;
import com.umc.product.member.domain.MemberSystemRoleType;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.support.IntegrationTestSupport;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;

@DisplayName("BlogInteractionController 통합 테스트")
class BlogInteractionControllerIntegrationTest extends IntegrationTestSupport {

    private static final String CONTENT_URL = "/api/v1/blog/contents/engineering/spring-boot-tips";
    private static final String BASE_URL = CONTENT_URL + "/comments";

    @Autowired
    SaveMemberPort saveMemberPort;

    @Autowired
    ChallengerFixture challengerFixture;

    @Autowired
    GisuFixture gisuFixture;

    @Autowired
    ChapterFixture chapterFixture;

    @Autowired
    SchoolFixture schoolFixture;

    @Autowired
    SaveBlogContentPort saveBlogContentPort;

    @Autowired
    MemberSystemRoleJpaRepository memberSystemRoleJpaRepository;

    private String authorToken;
    private String otherToken;
    private String adminToken;
    private Long authorMemberId;

    @BeforeEach
    void setUpAuth() {
        Gisu gisu = gisuFixture.비활성_기수(99L);
        Chapter chapter = chapterFixture.지부(gisu);
        School adminSchool = schoolFixture.지부에_소속된_학교("blog-admin-school", chapter);

        authorMemberId = createMember("author", adminSchool.getId());
        Long otherMemberId = createMember("other", adminSchool.getId());
        Long adminMemberId = createMember("admin", adminSchool.getId());

        challengerFixture.챌린저(authorMemberId, ChallengerPart.SPRINGBOOT, gisu.getId());
        challengerFixture.챌린저(otherMemberId, ChallengerPart.SPRINGBOOT, gisu.getId());
        challengerFixture.챌린저(adminMemberId, ChallengerPart.SPRINGBOOT, gisu.getId());
        memberSystemRoleJpaRepository.save(MemberSystemRole.create(adminMemberId, MemberSystemRoleType.SUPER_ADMIN));

        authorToken = mockToken("author-token", authorMemberId);
        otherToken = mockToken("other-token", otherMemberId);
        adminToken = mockToken("admin-token", adminMemberId);
        savePublishedContent();
    }

    private Long createMember(String name, Long schoolId) {
        return saveMemberPort.save(Member.create(
            name,
            name,
            name + "-blog@test.com",
            schoolId,
            null
        )).getId();
    }

    @Test
    @DisplayName("슈퍼 관리자는 콘텐츠를 생성하고 public 상세와 해시태그로 조회할 수 있다")
    void 슈퍼_관리자는_콘텐츠를_생성하고_public_상세와_해시태그로_조회할_수_있다() throws Exception {
        Map<String, Object> request = contentBody("cms-post", "CMS Post", "PUBLISHED", List.of("SpringBoot"));

        mockMvc.perform(post("/api/v1/blog/contents")
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/blog/contents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.slug").value("cms-post"))
            .andExpect(jsonPath("$.result.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.result.hashtags[0].slug").value("springboot"));

        mockMvc.perform(get("/api/v1/blog/contents/engineering/cms-post"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.title").value("CMS Post"))
            .andExpect(jsonPath("$.result.canonicalPath").value("/engineering/cms-post"));

        mockMvc.perform(get("/api/v1/blog/hashtags/springboot/contents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].slug").value("cms-post"));
    }

    @Test
    @DisplayName("SEO 경로 목록은 공개 콘텐츠 경로를 반환한다")
    void SEO_경로_목록은_공개_콘텐츠_경로를_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/blog/seo/paths"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.paths[0].type").value("content"))
            .andExpect(jsonPath("$.result.paths[0].path").value("/engineering/spring-boot-tips"));
    }

    @Test
    @DisplayName("공개 콘텐츠 목록은 공개되지 않은 콘텐츠 ID 커서를 거부한다")
    void 공개_콘텐츠_목록은_공개되지_않은_콘텐츠_ID_커서를_거부한다() throws Exception {
        BlogContent draft = saveBlogContentPort.save(BlogContent.create(
            BlogContentType.ENGINEERING,
            "draft-cursor",
            "Draft Cursor",
            null,
            null,
            "본문",
            BlogContentStatus.DRAFT,
            authorMemberId,
            null,
            null,
            null
        ));

        mockMvc.perform(get("/api/v1/blog/contents")
                .queryParam("cursor", draft.getId().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("해시태그 목록 커서는 type 필터 기준의 콘텐츠 수로 계산된다")
    void 해시태그_목록_커서는_type_필터_기준의_콘텐츠_수로_계산된다() throws Exception {
        createContent("engineering", "engineering-beta", "Engineering Beta", "PUBLISHED", List.of("Beta"));
        createContent("engineering", "engineering-alpha", "Engineering Alpha", "PUBLISHED", List.of("Alpha"));
        createContent("release", "release-alpha", "Release Alpha", "PUBLISHED", List.of("Alpha"));

        String firstResponse = mockMvc.perform(get("/api/v1/blog/hashtags")
                .queryParam("type", "engineering")
                .queryParam("size", "1")
                .queryParam("sort", "contentCount,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].slug").value("alpha"))
            .andExpect(jsonPath("$.result.content[0].contentCount").value(1))
            .andExpect(jsonPath("$.result.hasNext").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode firstResult = objectMapper.readTree(firstResponse).path("result");
        long firstHashtagId = firstResult.path("content").get(0).path("id").asLong();
        long nextCursor = firstResult.path("nextCursor").asLong();

        String secondResponse = mockMvc.perform(get("/api/v1/blog/hashtags")
                .queryParam("type", "engineering")
                .queryParam("size", "1")
                .queryParam("sort", "contentCount,desc")
                .queryParam("cursor", Long.toString(nextCursor)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].slug").value("beta"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode secondResult = objectMapper.readTree(secondResponse).path("result");
        assertThat(secondResult.path("content").get(0).path("id").asLong()).isNotEqualTo(firstHashtagId);
    }

    @Test
    @DisplayName("시리즈 콘텐츠 목록은 공개되지 않은 콘텐츠 ID 커서를 거부한다")
    void 시리즈_콘텐츠_목록은_공개되지_않은_콘텐츠_ID_커서를_거부한다() throws Exception {
        Long seriesId = createSeries("cursor-series", "Cursor Series");
        BlogContent published = saveBlogContentPort.save(BlogContent.create(
            BlogContentType.ENGINEERING,
            "series-published-cursor",
            "Series Published Cursor",
            null,
            null,
            "본문",
            BlogContentStatus.PUBLISHED,
            authorMemberId,
            null,
            null,
            null
        ));
        BlogContent draft = saveBlogContentPort.save(BlogContent.create(
            BlogContentType.ENGINEERING,
            "series-draft-cursor",
            "Series Draft Cursor",
            null,
            null,
            "본문",
            BlogContentStatus.DRAFT,
            authorMemberId,
            null,
            null,
            null
        ));
        mockMvc.perform(put("/api/v1/blog/series/" + seriesId + "/contents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(seriesContentsBody(List.of(published.getId(), draft.getId())))))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/blog/series/engineering/cursor-series/contents")
                .queryParam("cursor", draft.getId().toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("콘텐츠가 없어도 댓글 목록은 빈 커서 응답을 반환한다")
    void 콘텐츠가_없어도_댓글_목록은_빈_커서_응답을_반환한다() throws Exception {
        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").isArray())
            .andExpect(jsonPath("$.result.content").isEmpty())
            .andExpect(jsonPath("$.result.nextCursor").doesNotExist())
            .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    @DisplayName("콘텐츠 좋아요는 없는 콘텐츠 조회 시 빈 상태를 반환하고 공개 콘텐츠에서 토글된다")
    void 콘텐츠_좋아요는_없는_콘텐츠_조회_시_빈_상태를_반환하고_공개_콘텐츠에서_토글된다() throws Exception {
        String missingContentUrl = "/api/v1/blog/contents/engineering/missing-content";

        mockMvc.perform(get(missingContentUrl + "/like"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(false))
            .andExpect(jsonPath("$.result.likeCount").value(0));

        mockMvc.perform(post(CONTENT_URL + "/like")
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(true))
            .andExpect(jsonPath("$.result.likeCount").value(1));

        mockMvc.perform(get(CONTENT_URL + "/like")
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(true))
            .andExpect(jsonPath("$.result.likeCount").value(1));

        mockMvc.perform(post(CONTENT_URL + "/like")
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(false))
            .andExpect(jsonPath("$.result.likeCount").value(0));
    }

    @Test
    @DisplayName("본인 댓글만 수정할 수 있고 수정된 내용이 조회된다")
    void 본인_댓글만_수정할_수_있고_수정된_내용이_조회된다() throws Exception {
        Long commentId = createComment("수정 전 댓글", null, authorToken);

        mockMvc.perform(post(BASE_URL + "/" + commentId + "/like")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likeCount").value(1));

        mockMvc.perform(post(BASE_URL + "/" + commentId + "/like")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(false))
            .andExpect(jsonPath("$.result.likeCount").value(0));

        mockMvc.perform(post(BASE_URL + "/" + commentId + "/like")
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.likedByMe").value(true))
            .andExpect(jsonPath("$.result.likeCount").value(1));

        mockMvc.perform(patch(BASE_URL + "/" + commentId)
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody("다른 사용자 수정"))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch(BASE_URL + "/" + commentId)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateBody("수정 후 댓글"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.id").value(commentId))
            .andExpect(jsonPath("$.result.content").value("수정 후 댓글"))
            .andExpect(jsonPath("$.result.likeCount").value(1))
            .andExpect(jsonPath("$.result.likedByMe").value(false))
            .andExpect(jsonPath("$.result.canEdit").value(true))
            .andExpect(jsonPath("$.result.canDelete").value(true));

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].id").value(commentId))
            .andExpect(jsonPath("$.result.content[0].content").value("수정 후 댓글"))
            .andExpect(jsonPath("$.result.content[0].likeCount").value(1))
            .andExpect(jsonPath("$.result.content[0].canEdit").value(true))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(true));

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].canEdit").value(false))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(false));

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].canEdit").value(false))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(false));
    }

    @Test
    @DisplayName("댓글 목록은 size와 createdAt 정렬 기준에 맞춰 커서 페이지네이션된다")
    void 댓글_목록은_size와_createdAt_정렬_기준에_맞춰_커서_페이지네이션된다() throws Exception {
        Long firstId = createComment("첫 번째 댓글", null, authorToken);
        Long secondId = createComment("두 번째 댓글", null, authorToken);
        Long thirdId = createComment("세 번째 댓글", null, authorToken);

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "createdAt,asc")
                .queryParam("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].id").value(firstId))
            .andExpect(jsonPath("$.result.content[1].id").value(secondId))
            .andExpect(jsonPath("$.result.hasNext").value(true))
            .andExpect(jsonPath("$.result.nextCursor").value(secondId));

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "createdAt,asc")
                .queryParam("size", "2")
                .queryParam("cursor", secondId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].id").value(thirdId))
            .andExpect(jsonPath("$.result.hasNext").value(false));

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "createdAt,desc")
                .queryParam("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].id").value(thirdId))
            .andExpect(jsonPath("$.result.content[1].id").value(secondId))
            .andExpect(jsonPath("$.result.hasNext").value(true))
            .andExpect(jsonPath("$.result.nextCursor").value(secondId));
    }

    @Test
    @DisplayName("삭제된 댓글 ID를 커서로 사용하면 400을 반환한다")
    void 삭제된_댓글_ID를_커서로_사용하면_400을_반환한다() throws Exception {
        createComment("첫 번째 댓글", null, authorToken);
        Long secondId = createComment("두 번째 댓글", null, authorToken);
        createComment("세 번째 댓글", null, authorToken);

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "createdAt,asc")
                .queryParam("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.nextCursor").value(secondId));

        mockMvc.perform(delete(BASE_URL + "/" + secondId)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "createdAt,asc")
                .queryParam("size", "2")
                .queryParam("cursor", secondId.toString()))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("지원하지 않는 댓글 정렬 조건은 400을 반환한다")
    void 지원하지_않는_댓글_정렬_조건은_400을_반환한다() throws Exception {
        createComment("댓글", null, authorToken);

        mockMvc.perform(get(BASE_URL)
                .queryParam("sort", "likeCount,desc"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("대댓글이 있는 부모 댓글을 본인이 삭제하면 부모는 삭제 placeholder로 남고 대댓글은 조회된다")
    void 대댓글이_있는_부모_댓글을_본인이_삭제하면_부모는_삭제_placeholder로_남고_대댓글은_조회된다() throws Exception {
        Long parentId = createComment("부모 댓글", null, authorToken);
        Long replyId = createComment("대댓글", parentId, authorToken);

        mockMvc.perform(delete(BASE_URL + "/" + parentId)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].id").value(parentId))
            .andExpect(jsonPath("$.result.content[0].content").value("삭제된 댓글이에요"))
            .andExpect(jsonPath("$.result.content[0].deletionType").value("USER_DELETED"))
            .andExpect(jsonPath("$.result.content[0].canReply").value(false))
            .andExpect(jsonPath("$.result.content[0].likedByMe").value(false))
            .andExpect(jsonPath("$.result.content[0].likeCount").value(0))
            .andExpect(jsonPath("$.result.content[0].author").doesNotExist())
            .andExpect(jsonPath("$.result.content[0].canEdit").value(false))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(false))
            .andExpect(jsonPath("$.result.content[0].replies[0].id").value(replyId))
            .andExpect(jsonPath("$.result.content[0].replies[0].content").value("대댓글"));

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + authorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentBody("추가 대댓글", parentId))))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("삭제된 부모의 모든 대댓글이 사라지면 부모 댓글도 목록에서 제외된다")
    void 삭제된_부모의_모든_대댓글이_사라지면_부모_댓글도_목록에서_제외된다() throws Exception {
        Long parentId = createComment("부모 댓글", null, authorToken);
        Long replyId = createComment("대댓글", parentId, authorToken);

        mockMvc.perform(delete(BASE_URL + "/" + parentId)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_URL + "/" + replyId)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + authorToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content").isEmpty());
    }

    @Test
    @DisplayName("관리자 삭제는 관리자 placeholder로 조회되고 일반 사용자는 관리자 삭제를 호출할 수 없다")
    void 관리자_삭제는_관리자_placeholder로_조회되고_일반_사용자는_관리자_삭제를_호출할_수_없다() throws Exception {
        Long parentId = createComment("부모 댓글", null, authorToken);
        createComment("대댓글", parentId, authorToken);

        mockMvc.perform(delete(BASE_URL + "/" + parentId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE_URL + "/" + parentId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].content").value("관리자가 삭제한 댓글이에요"))
            .andExpect(jsonPath("$.result.content[0].deletionType").value("ADMIN_DELETED"))
            .andExpect(jsonPath("$.result.content[0].canReply").value(false))
            .andExpect(jsonPath("$.result.content[0].canEdit").value(false))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(false));
    }

    @Test
    @DisplayName("슈퍼 관리자가 본인 댓글을 삭제하면 본인 삭제 placeholder로 조회된다")
    void 슈퍼_관리자가_본인_댓글을_삭제하면_본인_삭제_placeholder로_조회된다() throws Exception {
        Long parentId = createComment("관리자 본인 댓글", null, adminToken);
        createComment("관리자 본인 대댓글", parentId, adminToken);

        mockMvc.perform(delete(BASE_URL + "/" + parentId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk());

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.content[0].content").value("삭제된 댓글이에요"))
            .andExpect(jsonPath("$.result.content[0].deletionType").value("USER_DELETED"))
            .andExpect(jsonPath("$.result.content[0].canReply").value(false))
            .andExpect(jsonPath("$.result.content[0].canEdit").value(false))
            .andExpect(jsonPath("$.result.content[0].canDelete").value(false));
    }

    private String mockToken(String token, Long memberId) {
        given(jwtTokenProvider.validateAccessToken(token)).willReturn(true);
        given(jwtTokenProvider.parseAccessToken(token)).willReturn(memberId);
        given(jwtTokenProvider.getRolesFromAccessToken(token)).willReturn(List.of());
        return token;
    }

    private void savePublishedContent() {
        saveBlogContentPort.save(BlogContent.create(
            BlogContentType.ENGINEERING,
            "spring-boot-tips",
            "Spring Boot Tips",
            "테스트 콘텐츠 요약",
            null,
            "테스트 콘텐츠 본문",
            BlogContentStatus.PUBLISHED,
            authorMemberId,
            null,
            null,
            null
        ));
    }

    private void createContent(String type, String slug, String title, String status, List<String> hashtags)
        throws Exception {
        mockMvc.perform(post("/api/v1/blog/contents")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(contentBody(type, slug, title, status, hashtags))))
            .andExpect(status().isOk());
    }

    private Long createSeries(String slug, String title) throws Exception {
        String response = mockMvc.perform(post("/api/v1/blog/series")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(seriesBody(slug, title))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response).path("result").path("id").asLong();
    }

    private Long createComment(String content, Long parentCommentId, String token) throws Exception {
        String response = mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentBody(content, parentCommentId))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        JsonNode result = objectMapper.readTree(response).path("result");
        return result.path("id").asLong();
    }

    private Map<String, Object> commentBody(String content, Long parentCommentId) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        body.put("anonymous", false);
        if (parentCommentId != null) {
            body.put("parentCommentId", parentCommentId);
        }
        return body;
    }

    private Map<String, Object> updateBody(String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("content", content);
        return body;
    }

    private Map<String, Object> contentBody(String slug, String title, String status, List<String> hashtags) {
        return contentBody("engineering", slug, title, status, hashtags);
    }

    private Map<String, Object> contentBody(
        String type,
        String slug,
        String title,
        String status,
        List<String> hashtags
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", type);
        body.put("slug", slug);
        body.put("title", title);
        body.put("summary", "요약");
        body.put("content", "본문");
        body.put("status", status);
        body.put("hashtags", hashtags);
        return body;
    }

    private Map<String, Object> seriesBody(String slug, String title) {
        Map<String, Object> body = new HashMap<>();
        body.put("type", "engineering");
        body.put("slug", slug);
        body.put("title", title);
        body.put("description", "설명");
        return body;
    }

    private Map<String, Object> seriesContentsBody(List<Long> contentIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("contentIds", contentIds);
        return body;
    }
}
