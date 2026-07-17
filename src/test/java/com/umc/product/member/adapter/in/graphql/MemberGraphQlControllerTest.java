package com.umc.product.member.adapter.in.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.execution.ErrorType;
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
import com.umc.product.common.domain.enums.MemberStatus;
import com.umc.product.global.config.GraphQlRuntimeWiringConfig;
import com.umc.product.global.exception.GraphQlExceptionAdvice;
import com.umc.product.global.exception.constant.CommonErrorCode;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.member.application.port.in.query.dto.SearchMemberItemV2Info;
import com.umc.product.member.application.port.in.query.dto.SearchMemberQuery;
import com.umc.product.member.application.port.in.query.dto.SearchMemberV2Result;
import com.umc.product.member.domain.exception.MemberDomainException;
import com.umc.product.member.domain.exception.MemberErrorCode;
import com.umc.product.organization.adapter.in.graphql.OrganizationGraphQlController;
import com.umc.product.organization.application.port.in.query.GetChapterUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuOrganizationUseCase;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.chapter.ChapterInfo;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

@GraphQlTest({MemberGraphQlController.class, OrganizationGraphQlController.class})
@Import({GraphQlRuntimeWiringConfig.class, GraphQlExceptionAdvice.class, CurrentMemberSecurityConfig.class})
@DisplayName("MemberGraphQlController")
class MemberGraphQlControllerTest {

    private static final Long REQUESTER_ID = 1L;
    private static final Long TARGET_ID = 2L;

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
    @DisplayName("me는 인증된 본인 회원 정보를 private view로 조회한다")
    void me는_인증된_본인_회원_정보를_private_view로_조회한다() {
        given(getMemberUseCase.getById(REQUESTER_ID)).willReturn(memberInfo(REQUESTER_ID));

        graphQlTester.document("""
                query {
                  me {
                    memberId
                    name
                    nickname
                    email
                    schoolId
                    schoolName
                    profileImageLink
                    status
                  }
                }
                """)
            .execute()
            .path("me.memberId").entity(String.class).isEqualTo("1")
            .path("me.email").entity(String.class).isEqualTo("member1@example.com")
            .path("me.status").entity(String.class).isEqualTo("ACTIVE");

        then(checkPermissionUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("member는 MEMBER READ 권한을 먼저 검사하고 public view로 조회한다")
    void member는_MEMBER_READ_권한을_먼저_검사하고_public_view로_조회한다() {
        given(getMemberUseCase.getById(TARGET_ID)).willReturn(memberInfo(TARGET_ID));

        graphQlTester.document("""
                query {
                  member(id: 2) {
                    memberId
                    name
                    nickname
                    email
                    status
                  }
                }
                """)
            .execute()
            .path("member.memberId").entity(String.class).isEqualTo("2")
            .path("member.name").entity(String.class).isEqualTo("member2")
            .path("member.email").valueIsNull()
            .path("member.status").valueIsNull();

        then(checkPermissionUseCase).should().checkOrThrow(REQUESTER_ID, memberReadPermission(TARGET_ID));
        then(getMemberUseCase).should().getById(TARGET_ID);
    }

    @Test
    @DisplayName("member 권한이 거부되면 회원 조회 usecase를 호출하지 않는다")
    void member_권한이_거부되면_회원_조회_usecase를_호출하지_않는다() {
        willThrow(new AccessDeniedException("회원 정보를 볼 권한이 없어요."))
            .given(checkPermissionUseCase)
            .checkOrThrow(REQUESTER_ID, memberReadPermission(TARGET_ID));

        graphQlTester.document("""
                query {
                  member(id: 2) {
                    memberId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "member", CommonErrorCode.FORBIDDEN));

        then(getMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("me는 인증 정보가 없으면 회원 조회 usecase를 호출하지 않는다")
    void me는_인증_정보가_없으면_회원_조회_usecase를_호출하지_않는다() {
        SecurityContextHolder.clearContext();

        graphQlTester.document("""
                query {
                  me {
                    memberId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "me", CommonErrorCode.FORBIDDEN));

        then(getMemberUseCase).shouldHaveNoInteractions();
        then(checkPermissionUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("members는 ID를 중복 제거하고 권한 확인 후 batch 조회한다")
    void members는_ID를_중복_제거하고_권한_확인_후_batch_조회한다() {
        SubjectAttributes subject = subject();
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, memberReadPermission(2L))).willReturn(true);
        given(checkPermissionUseCase.check(subject, memberReadPermission(3L))).willReturn(true);
        given(getMemberUseCase.findAllByIds(Set.of(2L, 3L))).willReturn(Map.of(
            2L, memberInfo(2L),
            3L, memberInfo(3L)
        ));

        graphQlTester.document("""
                query {
                  members(ids: [2, 2, 3]) {
                    memberId
                    email
                    status
                  }
                }
                """)
            .execute()
            .path("members[0].memberId").entity(String.class).isEqualTo("2")
            .path("members[0].email").valueIsNull()
            .path("members[0].status").valueIsNull()
            .path("members[1].memberId").entity(String.class).isEqualTo("3");

        then(getMemberUseCase).should().findAllByIds(Set.of(2L, 3L));
    }

    @Test
    @DisplayName("memberSearch는 요청자 권한 범위의 회원을 페이지로 조회하고 이메일을 마스킹한다")
    void memberSearch는_요청자_권한_범위의_회원을_페이지로_조회하고_이메일을_마스킹한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", 100L, ChallengerPart.SPRINGBOOT, 20L, 10L);
        PageRequest pageable = PageRequest.of(1, 2);
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            2L,
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
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 5)));

        graphQlTester.document("""
                query {
                  memberSearch(
                    input: { keyword: "kim", gisuId: 100, part: SPRINGBOOT, chapterId: 20, schoolId: 10 }
                    page: { page: 1, size: 2 }
                  ) {
                    content {
                      memberId
                      name
                      nickname
                      email
                      schoolId
                      profileImageLink
                      currentChallenger {
                        challengerId
                        gisuId
                        generation
                        part
                        challengerStatus
                      }
                      isAdminInActiveGisu
                      challengerRecords {
                        challengerId
                        gisuId
                        generation
                        part
                        challengerStatus
                      }
                    }
                    page
                    size
                    totalElements
                    totalPages
                    hasNext
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].memberId").entity(String.class).isEqualTo("2")
            .path("memberSearch.content[0].name").entity(String.class).isEqualTo("김회원")
            .path("memberSearch.content[0].nickname").entity(String.class).isEqualTo("키미")
            .path("memberSearch.content[0].email").entity(String.class).isEqualTo("mem****@example.com")
            .path("memberSearch.content[0].schoolId").entity(String.class).isEqualTo("10")
            .path("memberSearch.content[0].profileImageLink").entity(String.class)
                .isEqualTo("https://cdn.example.com/profile-2.png")
            .path("memberSearch.content[0].currentChallenger.challengerId").entity(String.class).isEqualTo("200")
            .path("memberSearch.content[0].currentChallenger.gisuId").entity(String.class).isEqualTo("100")
            .path("memberSearch.content[0].currentChallenger.generation").entity(String.class).isEqualTo("6")
            .path("memberSearch.content[0].currentChallenger.part").entity(String.class).isEqualTo("SPRINGBOOT")
            .path("memberSearch.content[0].currentChallenger.challengerStatus")
                .entity(String.class).isEqualTo("ACTIVE")
            .path("memberSearch.content[0].isAdminInActiveGisu").entity(Boolean.class).isEqualTo(true)
            .path("memberSearch.content[0].challengerRecords[0].challengerId")
                .entity(String.class).isEqualTo("201")
            .path("memberSearch.content[0].challengerRecords[0].gisuId").entity(String.class).isEqualTo("90")
            .path("memberSearch.content[0].challengerRecords[0].generation").entity(String.class).isEqualTo("5")
            .path("memberSearch.content[0].challengerRecords[0].part").entity(String.class).isEqualTo("NODEJS")
            .path("memberSearch.content[0].challengerRecords[0].challengerStatus")
                .entity(String.class).isEqualTo("GRADUATED")
            .path("memberSearch.page").entity(Integer.class).isEqualTo(1)
            .path("memberSearch.size").entity(Integer.class).isEqualTo(2)
            .path("memberSearch.totalElements").entity(Long.class).isEqualTo(5L)
            .path("memberSearch.totalPages").entity(Integer.class).isEqualTo(3)
            .path("memberSearch.hasNext").entity(Boolean.class).isEqualTo(true)
            .path("memberSearch").entity(Object.class)
                .satisfies(data -> assertThat(data.toString()).doesNotContain("member2@example.com"));

        then(searchMemberUseCase).should().searchByV2ForGraphQl(query, REQUESTER_ID, pageable);
    }

    @Test
    @DisplayName("memberSearch는 로컬 파트가 한 글자인 이메일도 원문을 노출하지 않는다")
    void memberSearch는_로컬_파트가_한_글자인_이메일도_원문을_노출하지_않는다() {
        String rawEmail = "a@umc.com";
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            2L,
            "김회원",
            "키미",
            rawEmail,
            10L,
            "중앙대학교",
            "https://cdn.example.com/profile-2.png",
            null,
            false,
            List.of()
        );
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 1)));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) {
                    content { email }
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].email").entity(String.class)
                .isEqualTo("[masked-email]")
            .path("memberSearch").entity(Object.class)
                .satisfies(data -> assertThat(data.toString()).doesNotContain(rawEmail));

        then(searchMemberUseCase).should().searchByV2ForGraphQl(query, REQUESTER_ID, pageable);
    }

    @Test
    @DisplayName("memberSearch는 null 이메일과 currentChallenger를 null로 유지한다")
    void memberSearch는_null_이메일과_currentChallenger를_null로_유지한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            2L,
            "김회원",
            "키미",
            null,
            10L,
            "중앙대학교",
            "https://cdn.example.com/profile-2.png",
            null,
            false,
            List.of()
        );
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 1)));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) {
                    content {
                      email
                      currentChallenger { challengerId }
                      challengerRecords { challengerId }
                    }
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].email").valueIsNull()
            .path("memberSearch.content[0].currentChallenger").valueIsNull()
            .path("memberSearch.content[0].challengerRecords").entityList(Object.class).hasSize(0);

        then(searchMemberUseCase).should().searchByV2ForGraphQl(query, REQUESTER_ID, pageable);
    }

    @Test
    @DisplayName("memberSearch는 page 입력을 생략하면 0페이지 20개를 조회한다")
    void memberSearch는_page_입력을_생략하면_기본값으로_조회한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(), pageable, 0)));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) {
                    content { memberId }
                    page
                    size
                    totalElements
                    totalPages
                    hasNext
                  }
                }
                """)
            .execute()
            .path("memberSearch.content").entityList(Object.class).hasSize(0)
            .path("memberSearch.page").entity(Integer.class).isEqualTo(0)
            .path("memberSearch.size").entity(Integer.class).isEqualTo(20)
            .path("memberSearch.totalElements").entity(Long.class).isEqualTo(0L)
            .path("memberSearch.totalPages").entity(Integer.class).isEqualTo(0)
            .path("memberSearch.hasNext").entity(Boolean.class).isEqualTo(false);

        then(searchMemberUseCase).should().searchByV2ForGraphQl(query, REQUESTER_ID, pageable);
    }

    @Test
    @DisplayName("memberSearch는 음수 page를 거부한다")
    void memberSearch는_음수_page를_거부한다() {
        assertInvalidMemberPage("page: -1, size: 20");
    }

    @Test
    @DisplayName("memberSearch는 0인 size를 거부한다")
    void memberSearch는_0인_size를_거부한다() {
        assertInvalidMemberPage("page: 0, size: 0");
    }

    @Test
    @DisplayName("memberSearch는 100을 초과한 size를 거부한다")
    void memberSearch는_100을_초과한_size를_거부한다() {
        assertInvalidMemberPage("page: 0, size: 101");
    }

    @Test
    @DisplayName("memberSearch는 offset이 10000인 페이지를 허용한다")
    void memberSearch는_offset이_10000인_페이지를_허용한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(100, 100);
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(), pageable, 0)));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }, page: { page: 100, size: 100 }) {
                    totalElements
                  }
                }
                """)
            .execute()
            .path("memberSearch.totalElements").entity(Long.class).isEqualTo(0L);

        then(searchMemberUseCase).should().searchByV2ForGraphQl(query, REQUESTER_ID, pageable);
    }

    @Test
    @DisplayName("memberSearch는 offset이 10000을 초과하면 BAD_REQUEST를 반환하고 검색하지 않는다")
    void memberSearch는_offset이_10000을_초과하면_거부한다() {
        assertInvalidMemberPage("page: 101, size: 100");
    }

    @Test
    @DisplayName("memberSearch 접근 권한이 거부되면 MEMBER-0014 FORBIDDEN 오류로 매핑한다")
    void memberSearch_접근_권한이_거부되면_MEMBER_0014_FORBIDDEN_오류로_매핑한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        willThrow(new MemberDomainException(MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED))
            .given(searchMemberUseCase)
            .searchByV2ForGraphQl(query, REQUESTER_ID, pageable);

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) { totalElements }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> {
                assertThat(errors).hasSize(1);
                assertThat(errors.get(0).getPath()).isEqualTo("memberSearch");
                assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.FORBIDDEN);
                assertThat(errors.get(0).getExtensions())
                    .containsEntry("code", MemberErrorCode.MEMBER_SEARCH_ACCESS_DENIED.getCode());
            });
    }

    @Test
    @DisplayName("memberSearch는 잘못된 part enum을 실행 전에 거부한다")
    void memberSearch는_잘못된_part_enum을_실행_전에_거부한다() {
        graphQlTester.document("""
                query {
                  memberSearch(input: { part: INVALID_PART }) { totalElements }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(searchMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("memberSearch는 숫자가 아닌 ID를 실행 전에 거부한다")
    void memberSearch는_숫자가_아닌_ID를_실행_전에_거부한다() {
        graphQlTester.document("""
                query {
                  memberSearch(input: { gisuId: "invalid-id" }) { totalElements }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        then(searchMemberUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("memberSearch는 school과 현재 및 이력 challenger의 gisu를 batch 조회한다")
    void memberSearch는_school과_현재_및_이력_challenger의_gisu를_batch_조회한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, ChallengerPart.SPRINGBOOT, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchMemberItemV2Info first = new SearchMemberItemV2Info(
            2L,
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
            false,
            List.of(
                new SearchMemberItemV2Info.Participation(
                    201L,
                    90L,
                    5L,
                    ChallengerPart.NODEJS,
                    ChallengerStatus.GRADUATED
                ),
                new SearchMemberItemV2Info.Participation(
                    202L,
                    100L,
                    6L,
                    ChallengerPart.SPRINGBOOT,
                    ChallengerStatus.ACTIVE
                )
            )
        );
        SearchMemberItemV2Info second = new SearchMemberItemV2Info(
            3L,
            "이회원",
            "리미",
            null,
            10L,
            "중앙대학교",
            null,
            new SearchMemberItemV2Info.PrimaryChallenger(
                300L,
                100L,
                6L,
                ChallengerPart.DESIGN,
                ChallengerStatus.ACTIVE
            ),
            false,
            List.of(
                new SearchMemberItemV2Info.Participation(
                    301L,
                    999L,
                    99L,
                    ChallengerPart.DESIGN,
                    ChallengerStatus.GRADUATED
                ),
                new SearchMemberItemV2Info.Participation(
                    302L,
                    null,
                    null,
                    ChallengerPart.DESIGN,
                    ChallengerStatus.GRADUATED
                )
            )
        );
        SearchMemberItemV2Info missingSchool = new SearchMemberItemV2Info(
            4L,
            "박회원",
            "파미",
            null,
            999L,
            "미등록학교",
            null,
            null,
            false,
            List.of()
        );
        SearchMemberItemV2Info nullSchool = new SearchMemberItemV2Info(
            5L,
            "최회원",
            "초미",
            null,
            null,
            null,
            null,
            null,
            false,
            List.of()
        );
        LinkedHashSet<Long> schoolIds = new LinkedHashSet<>(List.of(10L, 999L));
        LinkedHashSet<Long> gisuIds = new LinkedHashSet<>(List.of(100L, 90L, 999L));
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(
                new PageImpl<>(List.of(first, second, missingSchool, nullSchool), pageable, 4)
            ));
        given(getSchoolUseCase.listDetailsByIds(schoolIds))
            .willReturn(List.of(school(10L, "중앙대학교")));
        given(getGisuUseCase.getByIds(gisuIds))
            .willReturn(List.of(gisu(100L, 6L), gisu(90L, 5L)));

        graphQlTester.document("""
                query {
                  memberSearch(
                    input: { keyword: "kim", part: SPRINGBOOT }
                    page: { page: 0, size: 20 }
                  ) {
                    content {
                      memberId
                      name
                      nickname
                      email
                      profileImageLink
                      school {
                        schoolId
                        schoolName
                      }
                      currentChallenger {
                        challengerId
                        part
                        challengerStatus
                        gisu {
                          gisuId
                          generation
                        }
                      }
                      challengerRecords {
                        challengerId
                        part
                        challengerStatus
                        gisu {
                          gisuId
                          generation
                        }
                      }
                    }
                    page
                    size
                    totalElements
                    totalPages
                    hasNext
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].memberId").entity(String.class).isEqualTo("2")
            .path("memberSearch.content[0].name").entity(String.class).isEqualTo("김회원")
            .path("memberSearch.content[0].nickname").entity(String.class).isEqualTo("키미")
            .path("memberSearch.content[0].email").entity(String.class).isEqualTo("mem****@example.com")
            .path("memberSearch.content[0].profileImageLink").entity(String.class)
                .isEqualTo("https://cdn.example.com/profile-2.png")
            .path("memberSearch.content[0].school.schoolId").entity(String.class).isEqualTo("10")
            .path("memberSearch.content[0].school.schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("memberSearch.content[0].currentChallenger.challengerId")
                .entity(String.class).isEqualTo("200")
            .path("memberSearch.content[0].currentChallenger.part")
                .entity(String.class).isEqualTo("SPRINGBOOT")
            .path("memberSearch.content[0].currentChallenger.challengerStatus")
                .entity(String.class).isEqualTo("ACTIVE")
            .path("memberSearch.content[0].currentChallenger.gisu.gisuId")
                .entity(String.class).isEqualTo("100")
            .path("memberSearch.content[0].currentChallenger.gisu.generation")
                .entity(String.class).isEqualTo("6")
            .path("memberSearch.content[0].challengerRecords[0].gisu.gisuId")
                .entity(String.class).isEqualTo("90")
            .path("memberSearch.content[0].challengerRecords[0].gisu.generation")
                .entity(String.class).isEqualTo("5")
            .path("memberSearch.content[0].challengerRecords[0].challengerId")
                .entity(String.class).isEqualTo("201")
            .path("memberSearch.content[0].challengerRecords[0].part")
                .entity(String.class).isEqualTo("NODEJS")
            .path("memberSearch.content[0].challengerRecords[0].challengerStatus")
                .entity(String.class).isEqualTo("GRADUATED")
            .path("memberSearch.content[0].challengerRecords[1].gisu.gisuId")
                .entity(String.class).isEqualTo("100")
            .path("memberSearch.content[0].challengerRecords[1].gisu.generation")
                .entity(String.class).isEqualTo("6")
            .path("memberSearch.content[1].school.schoolId").entity(String.class).isEqualTo("10")
            .path("memberSearch.content[1].currentChallenger.gisu.gisuId")
                .entity(String.class).isEqualTo("100")
            .path("memberSearch.content[1].challengerRecords[0].gisu").valueIsNull()
            .path("memberSearch.content[1].challengerRecords[1].gisu").valueIsNull()
            .path("memberSearch.content[2].school").valueIsNull()
            .path("memberSearch.content[2].currentChallenger").valueIsNull()
            .path("memberSearch.content[3].school").valueIsNull()
            .path("memberSearch.page").entity(Integer.class).isEqualTo(0)
            .path("memberSearch.size").entity(Integer.class).isEqualTo(20)
            .path("memberSearch.totalElements").entity(Long.class).isEqualTo(4L)
            .path("memberSearch.totalPages").entity(Integer.class).isEqualTo(1)
            .path("memberSearch.hasNext").entity(Boolean.class).isEqualTo(false)
            .path("memberSearch").entity(Object.class)
                .satisfies(data -> assertThat(data.toString()).doesNotContain("member2@example.com"));

        then(getSchoolUseCase).should().listDetailsByIds(schoolIds);
        then(getGisuUseCase).should().getByIds(gisuIds);
    }

    @Test
    @DisplayName("memberSearch는 공용 Gisu의 chapters와 schools를 nested batch로 조회한다")
    void memberSearch는_공용_Gisu의_chapters와_schools를_nested_batch로_조회한다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            2L,
            "김회원",
            "키미",
            null,
            10L,
            "중앙대학교",
            null,
            new SearchMemberItemV2Info.PrimaryChallenger(
                200L,
                100L,
                6L,
                ChallengerPart.SPRINGBOOT,
                ChallengerStatus.ACTIVE
            ),
            false,
            List.of(new SearchMemberItemV2Info.Participation(
                201L,
                100L,
                6L,
                ChallengerPart.NODEJS,
                ChallengerStatus.GRADUATED
            ))
        );
        Set<Long> gisuIds = Set.of(100L);
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 1)));
        given(getGisuUseCase.getByIds(gisuIds)).willReturn(List.of(gisu(100L, 6L)));
        given(getChapterUseCase.listByGisuIds(gisuIds)).willReturn(Map.of(
            100L, List.of(new ChapterInfo(20L, "Ain 지부"))
        ));
        given(getSchoolUseCase.getSchoolListByGisuIds(gisuIds)).willReturn(Map.of(
            100L, List.of(school(10L, "중앙대학교"))
        ));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) {
                    content {
                      currentChallenger {
                        gisu {
                          chapters { chapterId chapterName }
                          schools { schoolId schoolName }
                        }
                      }
                      challengerRecords {
                        gisu {
                          chapters { chapterId chapterName }
                          schools { schoolId schoolName }
                        }
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].currentChallenger.gisu.chapters[0].chapterId")
                .entity(String.class).isEqualTo("20")
            .path("memberSearch.content[0].currentChallenger.gisu.chapters[0].chapterName")
                .entity(String.class).isEqualTo("Ain 지부")
            .path("memberSearch.content[0].currentChallenger.gisu.schools[0].schoolId")
                .entity(String.class).isEqualTo("10")
            .path("memberSearch.content[0].currentChallenger.gisu.schools[0].schoolName")
                .entity(String.class).isEqualTo("중앙대학교")
            .path("memberSearch.content[0].challengerRecords[0].gisu.chapters[0].chapterId")
                .entity(String.class).isEqualTo("20")
            .path("memberSearch.content[0].challengerRecords[0].gisu.schools[0].schoolId")
                .entity(String.class).isEqualTo("10");

        then(getGisuUseCase).should(times(1)).getByIds(gisuIds);
        then(getChapterUseCase).should(times(1)).listByGisuIds(gisuIds);
        then(getSchoolUseCase).should(times(1)).getSchoolListByGisuIds(gisuIds);
    }

    @Test
    @DisplayName("memberSearch nested parent ID가 모두 null이면 관련 조회를 호출하지 않는다")
    void memberSearch_nested_parent_ID가_모두_null이면_관련_조회를_호출하지_않는다() {
        SearchMemberQuery query = new SearchMemberQuery("kim", null, null, null, null);
        PageRequest pageable = PageRequest.of(0, 20);
        SearchMemberItemV2Info item = new SearchMemberItemV2Info(
            2L,
            "김회원",
            "키미",
            null,
            null,
            null,
            null,
            null,
            false,
            List.of(new SearchMemberItemV2Info.Participation(
                201L,
                null,
                null,
                ChallengerPart.NODEJS,
                ChallengerStatus.GRADUATED
            ))
        );
        given(searchMemberUseCase.searchByV2ForGraphQl(query, REQUESTER_ID, pageable))
            .willReturn(new SearchMemberV2Result(new PageImpl<>(List.of(item), pageable, 1)));

        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }) {
                    content {
                      school { schoolId }
                      currentChallenger { gisu { gisuId } }
                      challengerRecords { gisu { gisuId } }
                    }
                  }
                }
                """)
            .execute()
            .path("memberSearch.content[0].school").valueIsNull()
            .path("memberSearch.content[0].currentChallenger").valueIsNull()
            .path("memberSearch.content[0].challengerRecords[0].gisu").valueIsNull();

        then(getSchoolUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("members는 school과 challengers와 gisu를 nested field로 batch 조회한다")
    void members는_school과_challengers와_gisu를_nested_field로_batch_조회한다() {
        SubjectAttributes subject = subject();
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>(List.of(2L, 3L));
        LinkedHashSet<Long> schoolIds = new LinkedHashSet<>(List.of(10L, 11L));
        LinkedHashSet<Long> gisuIds = new LinkedHashSet<>(List.of(100L, 101L));
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, memberReadPermission(2L))).willReturn(true);
        given(checkPermissionUseCase.check(subject, memberReadPermission(3L))).willReturn(true);
        given(getMemberUseCase.findAllByIds(memberIds)).willReturn(Map.of(
            2L, memberInfo(2L, 10L),
            3L, memberInfo(3L, 11L)
        ));
        given(getSchoolUseCase.listDetailsByIds(schoolIds)).willReturn(List.of(
            school(10L, "중앙대학교"),
            school(11L, "숭실대학교")
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            2L, List.of(challenger(20L, 2L, 100L, ChallengerPart.SPRINGBOOT, ChallengerStatus.ACTIVE)),
            3L, List.of(challenger(30L, 3L, 101L, ChallengerPart.DESIGN, ChallengerStatus.GRADUATED))
        ));
        given(getGisuUseCase.getByIds(gisuIds)).willReturn(List.of(
            gisu(100L, 6L),
            gisu(101L, 7L)
        ));

        graphQlTester.document("""
                query {
                  members(ids: [2, 3]) {
                    memberId
                    school {
                      schoolId
                      schoolName
                    }
                    challengers {
                      challengerId
                      part
                      status
                      gisu {
                        gisuId
                        generation
                      }
                    }
                  }
                }
                """)
            .execute()
            .path("members[0].school.schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("members[0].challengers[0].part").entity(String.class).isEqualTo("SPRINGBOOT")
            .path("members[0].challengers[0].status").entity(String.class).isEqualTo("ACTIVE")
            .path("members[0].challengers[0].gisu.generation").entity(String.class).isEqualTo("6")
            .path("members[1].school.schoolName").entity(String.class).isEqualTo("숭실대학교")
            .path("members[1].challengers[0].status").entity(String.class).isEqualTo("GRADUATED")
            .path("members[1].challengers[0].gisu.generation").entity(String.class).isEqualTo("7");

        then(getSchoolUseCase).should().listDetailsByIds(schoolIds);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(memberIds);
        then(getGisuUseCase).should().getByIds(gisuIds);
    }

    @Test
    @DisplayName("members 중 하나라도 권한이 없으면 batch 조회를 호출하지 않는다")
    void members_중_하나라도_권한이_없으면_batch_조회를_호출하지_않는다() {
        SubjectAttributes subject = subject();
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, memberReadPermission(2L))).willReturn(true);
        given(checkPermissionUseCase.check(subject, memberReadPermission(3L))).willReturn(false);

        graphQlTester.document("""
                query {
                  members(ids: [2, 3]) {
                    memberId
                  }
                }
                """)
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "members", CommonErrorCode.FORBIDDEN));

        then(getMemberUseCase).shouldHaveNoInteractions();
        then(getSchoolUseCase).shouldHaveNoInteractions();
        then(getChallengerUseCase).shouldHaveNoInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    private void assertCommonError(
        List<org.springframework.graphql.ResponseError> errors,
        String path,
        CommonErrorCode code
    ) {
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).getPath()).isEqualTo(path);
        assertThat(errors.get(0).getExtensions()).containsEntry("code", code.getCode());
    }

    private void assertInvalidMemberPage(String pageInput) {
        graphQlTester.document("""
                query {
                  memberSearch(input: { keyword: "kim" }, page: { %s }) { totalElements }
                }
                """.formatted(pageInput))
            .execute()
            .errors()
            .satisfy(errors -> assertCommonError(errors, "memberSearch", CommonErrorCode.BAD_REQUEST));

        then(searchMemberUseCase).shouldHaveNoInteractions();
    }

    private MemberInfo memberInfo(Long memberId) {
        return memberInfo(memberId, 10L);
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
        return new ChallengerBasicInfo(challengerId, memberId, gisuId, part, status);
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
