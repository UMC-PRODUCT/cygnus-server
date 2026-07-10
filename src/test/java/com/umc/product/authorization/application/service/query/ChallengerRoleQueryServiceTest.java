package com.umc.product.authorization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.product.authorization.application.port.in.query.CheckChallengerAuthorityUseCase;
import com.umc.product.authorization.application.port.in.query.ListChallengerRoleUseCase;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleBasicInfo;
import com.umc.product.authorization.application.port.in.query.dto.ChallengerRoleInfo;
import com.umc.product.authorization.application.port.out.LoadChallengerRolePort;
import com.umc.product.authorization.domain.ChallengerRole;
import com.umc.product.common.domain.enums.ChallengerRoleType;
import com.umc.product.common.domain.enums.OrganizationType;
import com.umc.product.member.application.port.in.query.CheckMemberExistenceUseCase;
import com.umc.product.member.application.port.in.query.ListMemberSystemRoleUseCase;
import com.umc.product.member.application.port.in.query.dto.MemberSystemRoleInfo;
import com.umc.product.organization.application.port.in.query.GetGisuUseCase;
import com.umc.product.organization.application.port.in.query.dto.gisu.GisuInfo;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengerRoleQueryService")
class ChallengerRoleQueryServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long GISU_ID = 9L;
    private static final Long SCHOOL_ID = 30L;

    @Mock
    LoadChallengerRolePort loadChallengerRolePort;

    @Mock
    GetGisuUseCase getGisuUseCase;

    @Mock
    ListMemberSystemRoleUseCase listMemberSystemRoleUseCase;

    @Test
    @DisplayName("경량 역할 조회는 역할 범위만 매핑하고 기수 상세를 조회하지 않는다")
    void 경량_역할_조회는_기수_상세를_조회하지_않는다() {
        // given
        ChallengerRole centralRole = ChallengerRole.create(
            10L, ChallengerRoleType.CENTRAL_PRESIDENT, null, null, 3L
        );
        ChallengerRole schoolRole = ChallengerRole.create(
            11L, ChallengerRoleType.SCHOOL_VICE_PRESIDENT, 7L, null, 4L
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(centralRole, schoolRole));

        // when
        List<ChallengerRoleBasicInfo> result = sut().listBasicByMemberId(MEMBER_ID);

        // then
        assertThat(result).containsExactly(
            new ChallengerRoleBasicInfo(ChallengerRoleType.CENTRAL_PRESIDENT, OrganizationType.CENTRAL, null),
            new ChallengerRoleBasicInfo(ChallengerRoleType.SCHOOL_VICE_PRESIDENT, OrganizationType.SCHOOL, 7L)
        );
        then(loadChallengerRolePort).should().findByMemberId(MEMBER_ID);
        then(loadChallengerRolePort).shouldHaveNoMoreInteractions();
        then(getGisuUseCase).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("조회 전용 UseCase로 멤버의 역할 목록을 조회한다")
    void list_by_member_id() {
        ChallengerRoleQueryService sut = sut();
        ListChallengerRoleUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.SCHOOL_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));
        given(getGisuUseCase.getById(GISU_ID)).willReturn(new GisuInfo(GISU_ID, 10L, null, null, true));

        List<ChallengerRoleInfo> result = useCase.listByMemberId(MEMBER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().roleType()).isEqualTo(ChallengerRoleType.SCHOOL_PRESIDENT);
        assertThat(result.getFirst().gisu()).isEqualTo(10L);
    }

    @Test
    @DisplayName("전환 기간에는 기존 프로필 응답에 SUPER_ADMIN challenger role을 유지한다")
    void list_by_member_id_preserves_legacy_super_admin() {
        ChallengerRoleQueryService sut = sut();
        ListChallengerRoleUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.SUPER_ADMIN,
            null,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));
        given(getGisuUseCase.getById(GISU_ID)).willReturn(new GisuInfo(GISU_ID, 10L, null, null, true));

        List<ChallengerRoleInfo> result = useCase.listByMemberId(MEMBER_ID);

        assertThat(result).extracting(ChallengerRoleInfo::roleType)
            .containsExactly(ChallengerRoleType.SUPER_ADMIN);
    }

    @Test
    @DisplayName("권한 판정 UseCase로 특정 기수의 학교 회장단 여부를 확인한다")
    void check_school_core_in_gisu() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findRolesByMemberIdAndGisuId(MEMBER_ID, GISU_ID)).willReturn(List.of(role));

        boolean result = useCase.isSchoolCoreInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("권한 판정 UseCase로 중앙 총괄단 AnyGisu 정책을 명시적으로 확인한다")
    void check_central_core_in_any_gisu() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.CENTRAL_PRESIDENT,
            null,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));

        boolean result = useCase.isCentralCoreInAnyGisu(MEMBER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("권한 판정 UseCase로 학교 회장단 AnyGisu 정책을 명시적으로 확인한다")
    void check_school_core_in_any_gisu() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.SCHOOL_VICE_PRESIDENT,
            SCHOOL_ID,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));

        boolean result = useCase.isSchoolCoreInAnyGisu(MEMBER_ID, SCHOOL_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("조회 전용 UseCase로 챌린저별 역할 타입을 일괄 조회한다")
    void map_role_types_by_challenger_ids() {
        ChallengerRoleQueryService sut = sut();
        ListChallengerRoleUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.CENTRAL_PRESIDENT,
            null,
            null,
            GISU_ID
        );
        given(loadChallengerRolePort.findByChallengerIdIn(Set.of(10L))).willReturn(List.of(role));

        Map<Long, List<ChallengerRoleType>> result = useCase.mapRoleTypesByChallengerIds(Set.of(10L));

        assertThat(result).containsEntry(10L, List.of(ChallengerRoleType.CENTRAL_PRESIDENT));
    }

    @Test
    @DisplayName("SUPER_ADMIN은 member system role로 판정한다")
    void check_super_admin_by_member_system_role() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of(
            new MemberSystemRoleInfo(MEMBER_ID, "SUPER_ADMIN")
        ));

        boolean result = useCase.isSuperAdmin(MEMBER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("전환 기간에는 기존 challenger role의 SUPER_ADMIN도 판정한다")
    void check_super_admin_by_legacy_challenger_role() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        ChallengerRole role = ChallengerRole.create(
            10L,
            ChallengerRoleType.SUPER_ADMIN,
            null,
            null,
            GISU_ID
        );
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of());
        given(loadChallengerRolePort.findByMemberId(MEMBER_ID)).willReturn(List.of(role));

        boolean result = useCase.isSuperAdmin(MEMBER_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("member system role의 SUPER_ADMIN은 기수와 조직에 관계없이 상위 권한을 통과한다")
    void system_super_admin_has_all_hierarchical_authorities() {
        ChallengerRoleQueryService sut = sut();
        CheckChallengerAuthorityUseCase useCase = sut;
        given(listMemberSystemRoleUseCase.listByMemberId(MEMBER_ID)).willReturn(List.of(
            new MemberSystemRoleInfo(MEMBER_ID, "SUPER_ADMIN")
        ));

        assertThat(useCase.isCentralCoreInGisu(MEMBER_ID, GISU_ID)).isTrue();
        assertThat(useCase.isSchoolAdminInGisu(MEMBER_ID, GISU_ID, SCHOOL_ID)).isTrue();
        assertThat(useCase.isChapterPresidentInGisu(MEMBER_ID, GISU_ID, 20L)).isTrue();
    }

    @Test
    @DisplayName("삭제된 회원은 역할 저장소를 조회하지 않고 SUPER_ADMIN 권한을 거부한다")
    void deleted_member_cannot_use_legacy_super_admin() {
        CheckMemberExistenceUseCase missingMember = memberId -> false;
        ChallengerRoleQueryService sut = sut(missingMember);
        CheckChallengerAuthorityUseCase useCase = sut;

        assertThat(useCase.isSuperAdmin(MEMBER_ID)).isFalse();
        verifyNoInteractions(loadChallengerRolePort, listMemberSystemRoleUseCase);
    }

    private ChallengerRoleQueryService sut() {
        return sut(memberId -> true);
    }

    private ChallengerRoleQueryService sut(CheckMemberExistenceUseCase checkMemberExistenceUseCase) {
        return new ChallengerRoleQueryService(
            loadChallengerRolePort,
            getGisuUseCase,
            listMemberSystemRoleUseCase,
            checkMemberExistenceUseCase
        );
    }
}
