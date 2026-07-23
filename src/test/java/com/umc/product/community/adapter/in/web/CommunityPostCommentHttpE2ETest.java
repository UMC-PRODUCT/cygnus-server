package com.umc.product.community.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.global.security.JwtTokenProvider;
import com.umc.product.global.security.ParsedAccessToken;
import com.umc.product.member.application.port.out.SaveMemberPort;
import com.umc.product.member.domain.Member;
import com.umc.product.organization.domain.Chapter;
import com.umc.product.organization.domain.Gisu;
import com.umc.product.organization.domain.School;
import com.umc.product.storage.application.port.out.StoragePort;
import com.umc.product.support.TestContainersConfig;
import com.umc.product.support.fixture.ChallengerFixture;
import com.umc.product.support.fixture.ChapterFixture;
import com.umc.product.support.fixture.GisuFixture;
import com.umc.product.support.fixture.SchoolFixture;
import com.umc.product.support.isolation.DatabaseIsolation;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
@Testcontainers
@DatabaseIsolation
@DisplayName("커뮤니티 게시글·댓글 실제 HTTP E2E")
class CommunityPostCommentHttpE2ETest {

    private static final String TOKEN = "community-http-token";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SaveMemberPort saveMemberPort;

    @Autowired
    private GisuFixture gisuFixture;

    @Autowired
    private ChallengerFixture challengerFixture;

    @Autowired
    private ChapterFixture chapterFixture;

    @Autowired
    private SchoolFixture schoolFixture;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private FirebaseMessaging firebaseMessaging;

    @MockitoBean
    private StoragePort storagePort;

    private Member member;

    @BeforeEach
    void setUpAuthentication() {
        // given: 실제 HTTP 필터가 읽는 토큰만 테스트 경계에서 고정한다.
        Gisu gisu = gisuFixture.비활성_기수(9901L);
        Chapter chapter = chapterFixture.지부(gisu, "community-http-chapter");
        School school = schoolFixture.지부에_소속된_학교("community-http-school", chapter);
        member = saveMemberPort.save(Member.create(
            "community-member",
            "community-member",
            "community-member@test.com",
            school.getId(),
            null
        ));
        challengerFixture.챌린저(member.getId(), ChallengerPart.SPRINGBOOT, gisu.getId());

        given(jwtTokenProvider.parseAndValidateAccessToken(TOKEN))
            .willReturn(new ParsedAccessToken(member.getId(), List.of(), null));
    }

    @Test
    @DisplayName("실제 RANDOM_PORT HTTP에서 게시글·상세·좋아요·스크랩·댓글 흐름이 이어진다")
    void completePostAndCommentFlowOverActualHttp() {
        // when: 실제 포트로 게시글을 생성한다.
        JsonNode created = resultOf(exchange(
            "/api/v1/posts",
            HttpMethod.POST,
            Map.of(
                "title", "HTTP 직접 JPA 게시글",
                "content", "실제 서버 포트에서 확인하는 본문",
                "category", "FREE"
            )
        ));
        long postId = created.path("postId").asLong();

        // then: 생성 결과와 상세 조회가 같은 게시글을 가리킨다.
        assertThat(postId).isPositive();
        JsonNode detail = resultOf(exchange("/api/v1/posts/" + postId, HttpMethod.GET, null));
        assertThat(detail.path("postId").asLong()).isEqualTo(postId);
        assertThat(detail.path("title").asText()).isEqualTo("HTTP 직접 JPA 게시글");
        assertThat(detail.path("content").asText()).isEqualTo("실제 서버 포트에서 확인하는 본문");

        // when / then: 작성자가 PATCH 하면 수정 응답과 후속 상세 조회에 반영된다.
        JsonNode patched = resultOf(exchange(
            "/api/v1/posts/" + postId,
            HttpMethod.PATCH,
            Map.of(
                "title", "HTTP 직접 JPA 게시글 수정",
                "content", "PATCH 후 변경된 본문",
                "category", "INFORMATION"
            )
        ));
        assertThat(patched.path("postId").asLong()).isEqualTo(postId);
        assertThat(patched.path("title").asText()).isEqualTo("HTTP 직접 JPA 게시글 수정");
        assertThat(patched.path("content").asText()).isEqualTo("PATCH 후 변경된 본문");
        assertThat(patched.path("category").asText()).isEqualTo("INFORMATION");

        JsonNode patchedDetail = resultOf(exchange("/api/v1/posts/" + postId, HttpMethod.GET, null));
        assertThat(patchedDetail.path("title").asText()).isEqualTo("HTTP 직접 JPA 게시글 수정");
        assertThat(patchedDetail.path("content").asText()).isEqualTo("PATCH 후 변경된 본문");
        assertThat(patchedDetail.path("category").asText()).isEqualTo("INFORMATION");

        // when / then: 좋아요와 스크랩 토글은 실제 응답 수를 갱신한다.
        JsonNode liked = resultOf(exchange("/api/v1/posts/" + postId + "/like", HttpMethod.POST, null));
        assertThat(liked.path("liked").asBoolean()).isTrue();
        assertThat(liked.path("likeCount").asInt()).isEqualTo(1);

        JsonNode scrapped = resultOf(exchange("/api/v1/posts/" + postId + "/scrap", HttpMethod.POST, null));
        assertThat(scrapped.path("scrapped").asBoolean()).isTrue();
        assertThat(scrapped.path("scrapCount").asInt()).isEqualTo(1);

        // when: 같은 실제 포트에서 댓글을 생성하고 댓글 좋아요까지 수행한다.
        JsonNode comment = resultOf(exchange(
            "/api/v1/posts/" + postId + "/comments",
            HttpMethod.POST,
            Map.of("content", "실제 HTTP 댓글")
        ));
        long commentId = comment.path("commentId").asLong();
        assertThat(commentId).isPositive();

        JsonNode commentLike = resultOf(exchange(
            "/api/v1/posts/" + postId + "/comments/" + commentId + "/like",
            HttpMethod.POST,
            null
        ));
        assertThat(commentLike.path("liked").asBoolean()).isTrue();
        assertThat(commentLike.path("likeCount").asInt()).isEqualTo(1);

        // then: 댓글 목록과 상세 집계가 DB 왕복 후에도 유지된다.
        JsonNode comments = resultOf(exchange(
            "/api/v1/posts/" + postId + "/comments",
            HttpMethod.GET,
            null
        ));
        assertThat(comments.isArray()).isTrue();
        assertThat(comments.size()).isEqualTo(1);
        assertThat(comments.get(0).path("commentId").asLong()).isEqualTo(commentId);
        assertThat(comments.get(0).path("content").asText()).isEqualTo("실제 HTTP 댓글");

        JsonNode updatedDetail = resultOf(exchange("/api/v1/posts/" + postId, HttpMethod.GET, null));
        assertThat(updatedDetail.path("commentCount").asInt()).isEqualTo(1);
        assertThat(updatedDetail.path("likeCount").asInt()).isEqualTo(1);
        assertThat(updatedDetail.path("scrapCount").asInt()).isEqualTo(1);

        // when: 작성자가 실제 DELETE endpoint로 게시글을 삭제한다.
        ResponseEntity<JsonNode> deleted = exchange("/api/v1/posts/" + postId, HttpMethod.DELETE, null);

        // then: void 응답도 공통 성공 envelope를 유지한다.
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode deletedBody = deleted.getBody();
        assertThat(deletedBody).isNotNull();
        assertThat(deletedBody.path("success").asBoolean()).isTrue();
        assertThat(deletedBody.path("code").asText()).isEqualTo("COMMON200");
        assertThat(deletedBody.path("message").asText()).isEqualTo("성공입니다.");
        assertThat(deletedBody.path("result").isMissingNode()).isTrue();

        // then: 삭제된 게시글 상세 조회는 기존 COMMUNITY-0001 계약으로 실패한다.
        ResponseEntity<JsonNode> deletedDetail = exchange(
            "/api/v1/posts/" + postId,
            HttpMethod.GET,
            null
        );
        assertThat(deletedDetail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JsonNode deletedDetailBody = deletedDetail.getBody();
        assertThat(deletedDetailBody).isNotNull();
        assertThat(deletedDetailBody.path("success").asBoolean()).isFalse();
        assertThat(deletedDetailBody.path("code").asText()).isEqualTo("COMMUNITY-0001");
        assertThat(deletedDetailBody.path("message").asText())
            .isEqualTo("게시글을 찾을 수 없어요. 목록을 새로고침해주세요.");
        assertThat(deletedDetailBody.path("result").isMissingNode()).isTrue();
    }

    private ResponseEntity<JsonNode> exchange(String path, HttpMethod method, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        return restTemplate.exchange("http://localhost:%d%s".formatted(port, path), method, request, JsonNode.class);
    }

    private JsonNode resultOf(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.path("success").asBoolean()).isTrue();
        return body.path("result");
    }
}
