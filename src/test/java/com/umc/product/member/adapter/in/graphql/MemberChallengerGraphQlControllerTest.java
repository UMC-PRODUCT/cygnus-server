package com.umc.product.member.adapter.in.graphql;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.GraphQlTester;
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
import com.umc.product.global.graphql.relay.GlobalId;
import com.umc.product.global.graphql.relay.GlobalIdTypes;
import com.umc.product.global.graphql.relay.NodeGraphQlController;
import com.umc.product.global.security.CurrentMemberSecurityConfig;
import com.umc.product.global.security.MemberPrincipal;
import com.umc.product.member.application.port.in.query.GetMemberUseCase;
import com.umc.product.member.application.port.in.query.SearchMemberUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.GetSchoolUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;
import com.umc.product.organization.application.port.in.query.dto.school.SchoolDetailInfo;

@GraphQlTest({MemberGraphQlController.class, NodeGraphQlController.class})
@Import({
    GraphQlRuntimeWiringConfig.class,
    GraphQlExceptionAdvice.class,
    CurrentMemberSecurityConfig.class,
    MemberNodeFetcher.class
})
@DisplayName("Member Challenger GraphQL")
class MemberChallengerGraphQlControllerTest {

    private static final Long REQUESTER_ID = 1L;

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
    @DisplayName("members는 Challenger의 여러 tracks와 소속 정보를 batch 조회한다")
    void members는_Challenger의_여러_tracks와_소속_정보를_batch_조회한다() {
        SubjectAttributes subject = subject();
        LinkedHashSet<Long> memberIds = new LinkedHashSet<>(List.of(2L, 3L));
        LinkedHashSet<Long> schoolIds = new LinkedHashSet<>(List.of(10L, 11L));
        LinkedHashSet<Long> gisuIds = new LinkedHashSet<>(List.of(100L, 101L));
        given(checkPermissionUseCase.loadSubject(REQUESTER_ID)).willReturn(subject);
        given(checkPermissionUseCase.check(subject, memberReadPermission(2L))).willReturn(true);
        given(checkPermissionUseCase.check(subject, memberReadPermission(3L))).willReturn(true);
        given(getMemberUseCase.getById(2L)).willReturn(memberInfo(2L, 10L));
        given(getMemberUseCase.getById(3L)).willReturn(memberInfo(3L, 11L));
        given(getSchoolUseCase.listDetailsByIds(schoolIds)).willReturn(List.of(
            school(10L, "중앙대학교"),
            school(11L, "숭실대학교")
        ));
        given(getChallengerUseCase.getAllBasicByMemberIds(memberIds)).willReturn(Map.of(
            2L, List.of(challenger(
                20L,
                2L,
                100L,
                ChallengerPart.SPRINGBOOT,
                List.of(ChallengerTrack.WEB_PRODUCT_ENGINEER, ChallengerTrack.MOBILE_PRODUCT_ENGINEER),
                ChallengerStatus.ACTIVE
            )),
            3L, List.of(challenger(
                30L,
                3L,
                101L,
                ChallengerPart.DESIGN,
                List.of(ChallengerTrack.DESIGN),
                ChallengerStatus.GRADUATED
            ))
        ));
        given(getGisuUseCase.getByIds(gisuIds)).willReturn(List.of(
            gisu(100L, 6L),
            gisu(101L, 7L)
        ));

        graphQlTester.document("""
                query ($ids: [ID!]!) {
                  nodes(ids: $ids) {
                    ... on Member {
                      id
                      school {
                        id
                        schoolName
                      }
                      challengers {
                        challengerId
                        part
                        tracks
                        status
                        gisu {
                          id
                          generation
                        }
                      }
                    }
                  }
                }
                """)
            .variable("ids", List.of(
                GlobalId.encode(GlobalIdTypes.MEMBER, 2L),
                GlobalId.encode(GlobalIdTypes.MEMBER, 3L)
            ))
            .execute()
            .path("nodes[0].school.schoolName").entity(String.class).isEqualTo("중앙대학교")
            .path("nodes[0].challengers[0].part").entity(String.class).isEqualTo("SPRINGBOOT")
            .path("nodes[0].challengers[0].tracks").entityList(String.class)
            .containsExactly("WEB_PRODUCT_ENGINEER", "MOBILE_PRODUCT_ENGINEER")
            .path("nodes[0].challengers[0].status").entity(String.class).isEqualTo("ACTIVE")
            .path("nodes[0].challengers[0].gisu.generation").entity(Integer.class).isEqualTo(6)
            .path("nodes[1].school.schoolName").entity(String.class).isEqualTo("숭실대학교")
            .path("nodes[1].challengers[0].status").entity(String.class).isEqualTo("GRADUATED")
            .path("nodes[1].challengers[0].gisu.generation").entity(Integer.class).isEqualTo(7);

        then(getSchoolUseCase).should().listDetailsByIds(schoolIds);
        then(getChallengerUseCase).should().getAllBasicByMemberIds(memberIds);
        then(getGisuUseCase).should().getByIds(gisuIds);
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
        List<ChallengerTrack> tracks,
        ChallengerStatus status
    ) {
        return new ChallengerBasicInfo(challengerId, memberId, gisuId, part, tracks, status);
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
