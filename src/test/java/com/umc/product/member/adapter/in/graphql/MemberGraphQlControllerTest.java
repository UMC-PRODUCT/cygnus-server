package com.umc.product.member.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.product.authorization.application.port.in.CheckPermissionUseCase;
import com.umc.product.authorization.domain.PermissionType;
import com.umc.product.authorization.domain.ResourcePermission;
import com.umc.product.authorization.domain.ResourceType;
import com.umc.product.authorization.domain.SubjectAttributes;
import com.umc.product.challenger.application.port.in.query.GetChallengerUseCase;
import com.umc.product.challenger.application.port.in.query.dto.ChallengerBasicInfo;
import com.umc.product.common.domain.enums.ChallengerPart;
import com.umc.product.common.domain.enums.ChallengerStatus;
import com.umc.product.common.domain.enums.ChallengerTrack;
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeGraphQlController;
import com.umc.product.global.graphql.relay.RelayCursor;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;
import com.umc.product.organization.adapter.in.graphql.OrganizationGraphQlController;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

@GraphQlTest({
    MemberGraphQlController.class,
    OrganizationGraphQlController.class,
    NodeGraphQlController.class
})
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    CurrentMemberSecurityConfig.class,
    MemberNodeFetcher.class
})
@DisplayName("MemberGraphQlController")
class MemberGraphQlControllerTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long TARGET_ID = 2L;
    private static final String REQUESTER_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.MEMBER, REQUESTER_ID);
    private static final String TARGET_GLOBAL_ID = GlobalId.encode(GlobalIdTypes.MEMBER, TARGET_ID);

    @Autowired
    GraphQlTester graphQlTester;

    @MockitoBean
    GetMemberUseCase getMemberUseCase;

    @MockitoBean
    CheckPermissionUseCase checkPermissionUseCase;

    @MockitoBean
    GetSchoolUseCase getSchoolUseCase;

    @MockitoBean
    GetChallengerUseCase getChallengerUseCase;

    @MockitoBean
    GetGisuUseCase getGisuUseCase;

    @MockitoBean
    GetGisuOrganizationUseCase getGisuOrganizationUseCase;

    @MockitoBean
    GetChapterUseCase getChapterUseCase;

    @MockitoBean
    SearchMemberUseCase searchMemberUseCase;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new MemberPrincipal(REQUESTER_ID), null, List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("me는 전역 ID와 private 회원 정보를 반환한다")
    void me는_전역_ID와_private_회원_정보를_반환한다() {
        given(getMemberUseCase.getById(REQUESTER_ID)).willReturn(memberInfo(REQUESTER_ID, 10L));

        graphQlTester.document("""
                query {
                  me {
                    id
                    name
                    email
                    status
                  }
                }
                """)
            .execute()
            .path("me.id").entity(String.class).isEqualTo(REQUESTER_GLOBAL_ID)
            .path("me.name").entity(String.class).isEqualTo("member1")
            .path("me.email").entity(String.class).isEqualTo("member1@example.com")
            .path("me.status").entity(String.class).isEqualTo("ACTIVE");

        then(checkPermissionUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("member는 전역 ID를 디코딩하고 MEMBER READ 권한을 검사한다")
    void member는_전역_ID를_디코딩하고_MEMBER_READ_권한을_검사한다() {
        given(getMemberUseCase.getById(TARGET_ID)).willReturn(memberInfo(TARGET_ID, 10L));

        graphQlTester.document("""
                query ($id: ID!) {
                  member(id: $id) {
                    id
                    name
                    email
                    status
                  }
                }
                """)
            .variable("id", TARGET_GLOBAL_ID)
            .execute()
            .path("member.id").entity(String.class).isEqualTo(TARGET_GLOBAL_ID)
            .path("member.name").entity(String.class).isEqualTo("member2")
            .path("member.email").valueIsNull()
            .path("member.status").valueIsNull();

        then(checkPermissionUseCase).should().checkOrThrow(REQUESTER_ID, memberReadPermission(TARGET_ID));
        then(getMemberUseCase).should().getById(TARGET_ID);
    }

    @Test
    @DisplayName("member는 다른 타입의 전역 ID를 BAD_REQUEST로 거부한다")
    void member는_다른_타입의_전역_ID를_BAD_REQUEST로_거부한다() {
        graphQlTester.document("""
                query ($id: ID!) {
                  member(id: $id) { id }
                }
                """)
            .variable("id", GlobalId.encode(GlobalIdTypes.PROJECT, TARGET_ID))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "member", CommonErrorCode.BAD_REQUEST));

        then(checkPermissionUseCase).shouldHaveNoInteractions();
        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("member 권한이 거부되면 회원 조회를 호출하지 않는다")
    void member_권한이_거부되면_회원_조회를_호출하지_않는다() {
        willThrow(new AccessDeniedException("회원 정보를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, memberReadPermission(TARGET_ID));

        graphQlTester.document("""
                query ($id: ID!) {
                  member(id: $id) { id }
                }
                """)
            .variable("id", TARGET_GLOBAL_ID)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "member", CommonErrorCode.FORBIDDEN));

        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("members는 Relay Connection과 전역 ID 필터를 사용한다")
    void members는_Relay_Connection과_전역_ID_필터를_사용한다() {
        SearchMemberQuery expectedQuery = new SearchMemberQuery(
            "kim",
            100L,
            ChallengerPart.SPRINGBOOT,
            20L,
            10L
        );
        SearchMemberItemV2Info item = searchMemberItem();
        given(searchMemberUseCase.searchByV2ForGraphQl(
            eq(expectedQuery),
            eq(REQUESTER_ID),
            any(Pageable.class)
        )).willAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(2);
            return new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 5));
        });

        graphQlTester.document("""
                query ($filter: MemberFilterInput!, $first: Int!, $after: String!) {
                  members(filter: $filter, first: $first, after: $after) {
                    edges {
                      cursor
                      node {
                        memberId
                        name
                        email
                        currentChallenger {
                          challengerId
                          generation
                        }
                      }
                    }
                    pageInfo {
                      hasNextPage
                      hasPreviousPage
                    }
                    totalCount
                  }
                }
                """)
            .variable("filter", Map.of(
                "keyword", "kim",
                "gisuId", GlobalId.encode(GlobalIdTypes.GISU, 100L),
                "part", "SPRINGBOOT",
                "chapterId", GlobalId.encode(GlobalIdTypes.CHAPTER, 20L),
                "schoolId", GlobalId.encode(GlobalIdTypes.SCHOOL, 10L)
            ))
            .variable("first", 2)
            .variable("after", RelayCursor.encodeOffset(1))
            .execute()
            .path("members.edges[0].cursor").entity(String.class).isEqualTo(RelayCursor.encodeOffset(2))
            .path("members.edges[0].node.memberId").entity(String.class).isEqualTo(TARGET_GLOBAL_ID)
            .path("members.edges[0].node.name").entity(String.class).isEqualTo("김회원")
            .path("members.edges[0].node.email").entity(String.class).isEqualTo("mem****@example.com")
            .path("members.edges[0].node.currentChallenger.challengerId").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.CHALLENGER, 200L))
            .path("members.pageInfo.hasNextPage").entity(Boolean.class).isEqualTo(true)
            .path("members.pageInfo.hasPreviousPage").entity(Boolean.class).isEqualTo(true)
            .path("members.totalCount").entity(Long.class).isEqualTo(5L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(searchMemberUseCase).should().searchByV2ForGraphQl(
            eq(expectedQuery),
            eq(REQUESTER_ID),
            pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(2L);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("members는 school과 challenger의 gisu를 batch 조회한다")
    void members는_school과_challenger의_gisu를_batch_조회한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        SearchMemberItemV2Info item = searchMemberItem();
        given(searchMemberUseCase.searchByV2ForGraphQl(eq(query), eq(REQUESTER_ID), any(Pageable.class)))
            .willAnswer(invocation -> {
                Pageable pageable = invocation.getArgument(2);
                return new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 1));
            });
        given(getSchoolUseCase.listDetailsByIds(Set.of(10L)))
            .willReturn(List.of(school(10L, "중앙대학교")));
        given(getGisuUseCase.getByIds(Set.of(100L, 90L)))
            .willReturn(List.of(gisu(100L, 6L), gisu(90L, 5L)));

        graphQlTester.document("""
                query {
                  members(filter: { keyword: "kim" }) {
                    edges {
                      node {
                        school { id schoolName }
                        currentChallenger { gisu { id generation } }
                        challengerRecords { gisu { id generation } }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("members.edges[0].node.school.id").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.SCHOOL, 10L))
            .path("members.edges[0].node.currentChallenger.gisu.id").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 100L))
            .path("members.edges[0].node.currentChallenger.gisu.generation").entity(Integer.class).isEqualTo(6)
            .path("members.edges[0].node.challengerRecords[0].gisu.id").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 90L));

        then(getSchoolUseCase).should().listDetailsByIds(Set.of(10L));
        then(getGisuUseCase).should().getByIds(Set.of(100L, 90L));
    }

    @Test
    @DisplayName("Member nested 필드는 권한 확인 후 school과 challengers를 batch 조회한다")
    void Member_nested_필드는_권한_확인_후_school과_challengers를_batch_조회한다() {
        SubjectAttributes subject = subject();
        given(getMemberUseCase.getById(TARGET_ID)).willReturn(memberInfo(TARGET_ID, 10L));
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, memberReadPermission(TARGET_ID))).willReturn(true);
        given(getSchoolUseCase.listDetailsByIds(Set.of(10L)))
            .willReturn(List.of(school(10L, "중앙대학교")));
        given(getChallengerUseCase.getAllBasicByMemberIds(Set.of(TARGET_ID))).willReturn(Map.of(
            TARGET_ID,
            List.of(challenger(20L, TARGET_ID, 100L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE))
        ));
        given(getGisuUseCase.getByIds(Set.of(100L))).willReturn(List.of(gisu(100L, 6L)));

        graphQlTester.document("""
                query ($id: ID!) {
                  member(id: $id) {
                    school { id schoolName }
                    challengers {
                      challengerId
                      part
                      status
                      gisu { id generation }
                    }
                  }
                }
                """)
            .variable("id", TARGET_GLOBAL_ID)
            .execute()
            .path("member.school.schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("member.challengers[0].challengerId").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.CHALLENGER, 20L))
            .path("member.challengers[0].part").entity(String.class).isEqualTo("SPRINGBOOT")
            .path("member.challengers[0].gisu.id").entity(String.class)
                .isEqualTo(GlobalId.encode(GlobalIdTypes.GISU, 100L));

        then(getSchoolUseCase).should().listDetailsByIds(Set.of(10L));
        then(getChallengerUseCase).should().getAllBasicByMemberIds(Set.of(TARGET_ID));
        then(getGisuUseCase).should().getByIds(Set.of(100L));
    }

    @Test
    @DisplayName("members는 잘못된 페이지네이션 인자를 BAD_REQUEST로 거부한다")
    void members는_잘못된_페이지네이션_인자를_BAD_REQUEST로_거부한다() {
        graphQlTester.document("""
                query {
                  members(first: 101) { totalCount }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "members", CommonErrorCode.BAD_REQUEST));

        then(searchMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("members는 최대 offset을 초과한 커서를 BAD_REQUEST로 거부한다")
    void members는_최대_offset을_초과한_커서를_BAD_REQUEST로_거부한다() {
        graphQlTester.document("""
                query ($after: String!) {
                  members(first: 1, after: $after) { totalCount }
                }
                """)
            .variable("after", RelayCursor.encodeOffset(10_000L))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "members", CommonErrorCode.BAD_REQUEST));

        then(searchMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("members 검색 권한 오류는 도메인 오류 코드를 유지한다")
    void members_검색_권한_오류는_도메인_오류_코드를_유지한다() {
        given(searchMemberUseCase.searchByV2ForGraphQl(any(), eq(REQUESTER_ID), any(Pageable.class)))
            .willThrow(new AccessDeniedException("회원 검색 권한이 없어요."));

        graphQlTester.document("""
                query {
                  members(filter: { keyword: "kim" }) { totalCount }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "members", CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("node는 Member 전역 ID로 회원을 재조회한다")
    void node는_Member_전역_ID로_회원을_재조회한다() {
        given(getMemberUseCase.getById(TARGET_ID)).willReturn(memberInfo(TARGET_ID, 10L));

        graphQlTester.document("""
                query ($id: ID!) {
                  node(id: $id) {
                    id
                    ... on Member { name email }
                  }
                }
                """)
            .variable("id", TARGET_GLOBAL_ID)
            .execute()
            .path("node.id").entity(String.class).isEqualTo(TARGET_GLOBAL_ID)
            .path("node.name").entity(String.class).isEqualTo("member2")
            .path("node.email").valueIsNull();

        then(checkPermissionUseCase).should().checkOrThrow(REQUESTER_ID, memberReadPermission(TARGET_ID));
        then(getMemberUseCase).should().getById(TARGET_ID);
    }

    @Test
    @DisplayName("me와 동일 회원 node는 같은 private 필드 값을 반환한다")
    void me와_동일_회원_node는_같은_private_필드_값을_반환한다() {
        given(getMemberUseCase.getById(REQUESTER_ID)).willReturn(memberInfo(REQUESTER_ID, 10L));

        graphQlTester.document("""
                query ($id: ID!) {
                  me { id email status }
                  node(id: $id) {
                    id
                    ... on Member { email status }
                  }
                }
                """)
            .variable("id", REQUESTER_GLOBAL_ID)
            .execute()
            .path("me.id").entity(String.class).isEqualTo(REQUESTER_GLOBAL_ID)
            .path("node.id").entity(String.class).isEqualTo(REQUESTER_GLOBAL_ID)
            .path("me.email").entity(String.class).isEqualTo("member1@example.com")
            .path("node.email").entity(String.class).isEqualTo("member1@example.com")
            .path("me.status").entity(String.class).isEqualTo("ACTIVE")
            .path("node.status").entity(String.class).isEqualTo("ACTIVE");

        then(checkPermissionUseCase).should().checkOrThrow(REQUESTER_ID, memberReadPermission(REQUESTER_ID));
    }

    private void assertCommonError(
        List<org.springframework.graphql.ResponseError> errors,
        String path,
        CommonErrorCode code
    ) {
        assertThat(errors).anySatisfy(error -> {
            assertThat(error.getPath()).isEqualTo(path);
            assertThat(error.getExtensions()).containsEntry("code", code.getCode());
        });
    }

    private MemberInfo memberInfo(Long memberId, Long schoolId) {
        return MemberInfo.builder()
            .id(memberId)
            .name("member" + memberId)
            .nickname("nick" + memberId)
            .email("member" + memberId + "@example.com")
            .schoolId(schoolId)
            .schoolName("중앙대학교")
            .profileImageId("profile-" + memberId)
            .profileImageLink("https://cdn.example.com/profile-" + memberId + ".png")
            .status(MemberStatus.ACTIVE)
            .roles(List.of())
            .build();
    }

    private SearchMemberItemV2Info searchMemberItem() {
        return new SearchMemberItemV2Info(
            TARGET_ID,
            "김회원",
            "키미",
            "member2@example.com",
            10L,
            "중앙대학교",
            "https://cdn.example.com/profile-2.png",
            new SearchMemberItemV2Info.PrimaryChallenger(
                200L,
                100L,
                6L,
                ChallengerPart.SPRINGBOOT,
                ChallengerStatus.ACTIVE
            ),
            true,
            List.of(new SearchMemberItemV2Info.Participation(
                201L,
                90L,
                5L,
                ChallengerPart.NODEJS,
                ChallengerStatus.GRADUATED
            ))
        );
    }

    private SubjectAttributes subject() {
        return SubjectAttributes.builder()
            .memberId(REQUESTER_ID)
            .schoolId(10L)
            .gisuChallengerInfos(List.of())
            .roleAttributes(List.of())
            .build();
    }

    private ResourcePermission memberReadPermission(Long memberId) {
        return ResourcePermission.of(ResourceType.MEMBER, memberId, PermissionType.READ);
    }

    private SchoolDetailInfo school(Long schoolId, String schoolName) {
        return new SchoolDetailInfo(
            1L,
            "1지부",
            schoolName,
            null,
            schoolId,
            "비고",
            null,
            List.of(),
            true,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-01-02T00:00:00Z")
        );
    }

    private ChallengerBasicInfo challenger(
        Long challengerId,
        Long memberId,
        Long gisuId,
        ChallengerPart part,
        ChallengerStatus status
    ) {
        return new ChallengerBasicInfo(
            challengerId,
            memberId,
            gisuId,
            part,
            List.of(ChallengerTrack.from(part)),
            status
        );
    }

    private GisuInfo gisu(Long gisuId, Long generation) {
        return new GisuInfo(
            gisuId,
            generation,
            Instant.parse("2026-01-01T00:00:00Z"),
            Instant.parse("2026-12-31T00:00:00Z"),
            true
        );
    }
}
