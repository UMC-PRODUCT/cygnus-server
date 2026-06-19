package com.umc.product.authorization.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks
    ChallengerRoleQueryService service;

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
        List<ChallengerRoleBasicInfo> result = service.listBasicByMemberId(MEMBER_ID);

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
        ListChallengerRoleUseCase useCase = service;
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
    @DisplayName("권한 판정 UseCase로 특정 기수의 학교 회장단 여부를 확인한다")
    void check_school_core_in_gisu() {
        CheckChallengerAuthorityUseCase useCase = service;
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
    @DisplayName("조회 전용 UseCase로 챌린저별 역할 타입을 일괄 조회한다")
    void map_role_types_by_challenger_ids() {
        ListChallengerRoleUseCase useCase = service;
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
}
